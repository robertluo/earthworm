(ns earthworm.util.bean)

;;转换函数

(defn rm-defaults
  "将 m 中是默认值(由 pred 函数确定)的项目去掉. 如果整个 map 都是
   默认值, 则返回 nil"
  [pred m]
  (seq (filter (fn [[_ v]] (not (pred v))) m)))

(defn default? [x]
  (or (and (number? x) (zero? x))
      (false? x)
      (= x (Boolean. false))
      (and (coll? x) (not (seq x)))
      (nil? x)))

(def rm-nils (partial rm-defaults default?))

(defn process-keys
  [f m]
  (map (fn [[k v]] [(f k) v]) m))

(defn ren-keys
  "用 key-map 来查找 m 中的 keys, 如果有匹配项就替换, 返回替换后的
   map"
  [key-map m]
  (process-keys #(get key-map % %) m))

(defn keys-with-ns
  "给 m 中的每一个 key 加上 ns 前缀.
  例如: (keys-with-ns [[:name :foo]] \"user\") => [[:user/name \"user\"]]"
  [ns m]
  (process-keys #(keyword ns (name %)) m))

(defn pack
  "将 m 整理成一个 map"
  [m]
  (when (seq m)
    (into {} m)))

(defn rm-v-nils [m]
  (filter (fn [[_ v]] (not (default? v))) m))

(defn map-rm-nils [m]
  (-> m rm-nils rm-v-nils pack))

(defn remove-key-ns
  "移除keyword的命名空间"
  [k]
  (if (keyword? k)
    (keyword (name k))
    k))

;;=================================
;; Java <-> Clojure 存取

(defn- fn-names [name]
  [(symbol (str "from-" name))
   (symbol (str "to-" name))])

(defmacro defbean
  "定义 Java Bean 到 clojure map 的转换函数. name 是数据的名称, 如 java.util.Date
   可以起名为 date, 生成的函数会有 from- + name 将 JavaBean 转为 map, 而 to- +name
   的函数将 map 转为 Java Bean. clazz 是 Java Bean 的类名, 用于 to- 函数中生成一个
   默认未设置属性的 bean. desc 是属性的说明, 它是一个 vector, 其中每一项是三项目的
   vector, 第1项是 map 的键名称, 第2,3 项是 Java 属性的 accessor 和 mutator 方法说明.
   Java 方法说明可以是方法名称本身, 也可以是一个两项的 vector, 第一项是方法名, 第二项
   是一个单一参数的函数: 对于 accessor, 这个函数在 accessor 后调用, 实际参数是 accessor
   的返回结果. 对于 mutator, 这个参数在 mutator 之前调用, 实际参数是 clojure map
   的相应属性值.

   例如: (defbean date java.util.Date
           [[:date/month .getMonth .setMonth]
            [:date/year [.getYear inc-year] [.setYear dec-year]]])

  其中： inc-year, dec-year 是单参数函数.

   将定义两个函数, from-date 和 to-date."
  [name clazz desc]
  (let [[from-macro to-macro] (fn-names name)
        obj-sym (gensym "obj")
        obj (with-meta 'obj-sym {:tag clazz})
        finside (mapv (fn [[k getter]]
                        (if (vector? getter)
                          `[~k (some-> ~obj ~(first getter) ~(last getter))]
                          `[~k (~getter ~obj)]))
                      desc)
        bean (gensym "bean")
        sm (gensym "sm")
        tinside (map (fn [[k _ setter]]
                       (let [v (gensym "v")]
                         `(when (contains? ~sm ~k)
                            (let [~v (get ~sm ~k)]
                              ~(if (vector? setter)
                                 `(~(first setter) ~bean (~(last setter) ~v))
                                 `(~setter ~bean ~v))))))
                     desc)]
    `(do
       (defn ~from-macro [~(with-meta obj {:tag clazz})]
         (when ~obj
           (->> ~finside map-rm-nils)))
       (defn ~to-macro [~sm]
         (when ~sm
           (let [~bean (new ~clazz)]
             (do ~@tinside
                 ~bean)))))))