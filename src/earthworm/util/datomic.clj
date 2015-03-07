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
  "给一组实体 ents 在 part 这个数据库分区中顺序生成临时 id"
  [part ents]
  (map #(assoc %1 :db/id (d/tempid part %2)) ents (iterate dec -1)))

(defn ensure-db
  "建立数据库结构和种子数据。结构为 schema, 如果有种子数据 seed-data，也记入数据库."
  [conn schema & [seed-data]]
  @(d/transact conn schema)
  (when seed-data
    @(d/transact conn seed-data)))

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

(def builtin-db-funcs
  "内嵌在每个数据库的函数"
  (entities
    :db.part/user
    [{:db/ident :db.fn/seq-id :db/fn seq-id :db/doc (-> #'seq-id meta :doc)}
     {:db/ident :db.fn/atom-plus :db/fn atom-plus :db/doc (-> #'atom-plus meta :doc)}]))

(defn init-conn
  "用数据库定义来新生成数据库, 返回到它的连接. 如果不指定 uri, 将随机生成一个内存数据库. 用于测试."
  ([db-def]
    (let [uri (str "datomic:mem:test" (rand-int 10000))]
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

(defn future-db [db trans]
  (:db-after (d/with db trans)))