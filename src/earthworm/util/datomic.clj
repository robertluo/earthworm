(ns earthworm.util.datomic
  "方便性的处理函数。对于每个 datomic 程序都有用!"
  (:require [datomic.api :as d]))

(defn assoc-when [m k v]
  (if v (assoc m k v) m))

(defn attr
  "用缩略形式创建 schema 中的属性:
   每个属性必须有 ident 名称, type 类型和 doc 文档.
   可选用 :cardinality 指定数量以及 :unique 指定唯一性"
  [ident type doc & {:as options}]
  (merge {:db/ident              ident
          :db/valueType          type
          :db/doc                doc
          :db/cardinality        :db.cardinality/one
          :db.install/_attribute :db.part/db} options))

(defn entities
      "给一组实体 ents中没有包含 :db/id 字段的实体 在 part 这个数据库分区中顺序生成临时 id"
      [part ents]
      (map #(if (:db/id %1)
             %1
             (assoc %1 :db/id (d/tempid part %2)))
           ents (iterate dec -1)))

(defn ensure-db
  "建立数据库结构和种子数据。结构为 schema, 如果有种子数据 seed-data，也记入数据库."
  [conn schema & [seed-data]]
  @(d/transact conn schema)
  (when seed-data
    @(d/transact conn seed-data)))

;; 常用内置数据库函数
;; =========================

(def seq-id
  "Transaction 函数. 给 eid 实体构造的 id-field 设置一个顺序的 id. 相当于传统数据库的
   sequence 或者自动增长功能.
   要求在数据的种子数据中建立一个实体有 :db/ident (函数中用 next-id 指定),
   这个实体有属性 next-value 来存储当前的最后 id 值."
  (d/function {:lang   "clojure"
               :params '[db next-id next-value eid id-field]
               :code   '(let [val (get (d/entity db next-id) next-value 0)]
                          [{:db/id next-id next-value (inc val)}
                           {:db/id eid id-field val}])}))

(def atom-plus
  "在 db/id 为 eid 的实体的属性 attr 上增加 amount (先取再加)"
  (d/function {:lang   "clojure"
               :params '[db eid attr amount]
               :code   '(let [val (get (d/entity db eid) attr 0)]
                          [{:db/id eid attr (+ val amount)}])}))

(def lookup-eid
  "在数据库中查找符合定义的数据 id, 提供一个通用的查找一级和二级数据id 的方法
  lookup-define 定义如下: [entity-id sub-entity-define]
  sub-entity-define 如果为空, 那么只查找第一级的数据id, 否则查找下一级的数据id
  entity-id: 可以是 id 也可以是 lookup-ref 形式
  sub-entity-define: 可选, 格式为 [attr-key define-map]
  例子: [ [user/name \"foo\"] ]
       [ 17592186045516 [:user-levels {:level/game :game.type/ddz}]]
       [ [user/name \"foo\"] [:user-relations {:relation/type :friend :relation/userName \"my_friends\"}] ]"
  #db/fn
      {:lang :clojure
       :params [db lookup-define]
       :code (let [id (first lookup-define)
                   [component-key kvs] (second lookup-define)]
               (if component-key
                 (let [where-cluase (mapv #(apply vector '?l %) kvs)]
                   (ffirst (d/q {:find  ['?l]
                                 :in    ['$ '?id]
                                 :where (concat [['?id component-key '?l]]
                                                where-cluase)}
                                db id)))
                 (-> (d/entity db id) :db/id)))})

(def smart-atom-plus
  "在数据库中对指定属性 attr 上增加 amount (先取再加) 的高级版本,
   lookup-define 使用 lookup-eid 中的定义.
   如果指定的属性不存在, 该函数会自动添加一条新数据"
  #db/fn
      {:lang :clojure
       :params [db lookup-define attr amount]
       :code (let [fn-lookup-eid (-> (d/entity db :db.fn/lookup-eid) :db/fn)
                   eid (fn-lookup-eid db lookup-define)
                   [parent-id [component-key component-value]] lookup-define]
               (if eid
                 [[:db.fn/atom-plus eid attr amount]]
                 [{:db/id parent-id
                   component-key (assoc component-value attr amount)}]))})

(def create-or-update
  "在数据库中修改或者添加指定的数据
  lookup-define 使用 lookup-eid 中的定义."
  #db/fn
      {:lang :clojure
       :params [db lookup-define new-data]
       :code (let [fn-lookup-eid (-> (d/entity db :db.fn/lookup-eid) :db/fn)
                   eid (fn-lookup-eid db lookup-define)
                   [parent-id [component-key component-value]] lookup-define]
               (if eid
                 [(assoc new-data :db/id eid)]
                 [{:db/id parent-id
                   component-key (merge component-value new-data)}]))})

(def update-set
  "更新 set 类型(cadinality/many, isComponent/true)的属性值, 自动生成 retract 和 add transaction 数据"
  #db/fn
      {:lang   :clojure
       :params [db eid attr new-data]
       :code   (let [old (set (get (d/entity db eid) attr))
                     new-data (set new-data)
                     to-be-retract (clojure.set/difference old new-data)
                     to-be-add (clojure.set/difference new-data old)]
                 (concat (map (fn [v] [:db/retract eid attr v]) to-be-retract)
                         (when (seq to-be-add) [{:db/id eid attr (vec to-be-add)}])))})

(def create-unique-data
  "根据指定的unique键, 创建唯一一条数据, 若unique指定数据已经被创建, 则本条数据不会写入数据库;
  本函数旨满足要指定组合型唯一约束的场景, datomic数据库定义时不能指定组合唯一约束.
  lookup-define 使用 lookup-eid 中的定义."
  #db/fn
      {:lang :clojure
       :params [db lookup-define new-data]
       :code (let [fn-lookup-eid (-> (d/entity db :db.fn/lookup-eid) :db/fn)
                   eid (fn-lookup-eid db lookup-define)]
               (when-not eid
                 new-data))})

;;============================

(defn mk-db-schema
  "datomic 属性定义的 schema 有很多重复的部分, 通过提供大多数属性的默认值,
   将独特的东西放进 smeta, 可以简化定义. smeta 和 schema 一样, 是一个 map
   的 vector. 每一项唯一必须得是有 :db/ident 定义"
  [smeta]
  (let [defaults {:db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db.install/_attribute :db.part/db}]
    (mapv #(merge defaults {:db/id (d/tempid :db.part/db %2)} %1)
          smeta (iterate dec -1))))

(defn first-tempid
  "返回事务结果 tx 中的第一个 tempid 值"
  [tx]
  (-> tx :tempids vals first))

(defn when-db [ent]
  (when (:db/id ent) ent))

(defrecord DatabaseDefinition [schema seed sample])

(def def-db
  "定义一个数据库"
  ->DatabaseDefinition)

(defn db-fn
  "用一个 var(用 #db.fn 定义）返回一个可安装的数据库函数数据."
  [var]
  {:db/ident (keyword "db.fn" (-> var meta :name str)) :db/fn @var :db/doc (-> var meta :doc)})

(def builtin-db-funcs
  "内嵌在每个数据库的函数"
  (entities
    :db.part/user
    (mapv db-fn [#'seq-id #'atom-plus #'lookup-eid #'smart-atom-plus #'create-or-update #'update-set #'create-unique-data])))

(defn init-conn
  "用数据库定义来新生成数据库, 返回到它的连接. 如果不指定 uri, 将随机生成一个内存数据库. 用于测试."
  ([db-def]
    (let [uri (str "datomic:mem:test" (java.util.UUID/randomUUID))]
      (init-conn db-def uri true)))
  ([{:keys [schema seed sample]} uri sample?]
    (d/create-database uri)
    (let [conn (d/connect uri)
          sample (when sample? sample)]
      (doseq [tran [builtin-db-funcs schema seed sample]]
        @(d/transact conn tran))
      conn)))

(defn init-db [def-db]
  (some-> def-db init-conn d/db))

(defn future-db
  "给定事务数据 trans, 返回最终的 db. 用于测试"
  [db & trans]
  (reduce #(:db-after (d/with %1 %2)) db trans))