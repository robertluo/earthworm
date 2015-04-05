(ns earthworm.util.datomic-test
  (:require [clojure.test :refer :all]
            [earthworm.util.datomic :refer :all]
            [datomic.api :as d]))

(def sample-def
  (def-db (entities :db.part/db
                    [(attr :user/name :db.type/string "user name" :db/unique :db.unique/identity)
                     (attr :user/height :db.type/long "height of user in cm")
                     (attr :user/relatives :db.type/ref "Relatives" :db/cardinality :db.cardinality/many
                           :db/isComponent true)
                     (attr :user/friends :db.type/string "friends" :db/cardinality :db.cardinality/many)

                     (attr :relative/type :db.type/ref "Relative's type")
                     (attr :relative/user :db.type/ref "Relative is an user")])
          (entities :db.part/user [{:db/ident :relative.type/dad} {:db/ident :relative.type/mom}])
          (entities :db.part/user [{:user/name "liubei" :user/height 136 :db/id #db/id[:db.part/user -1]
                                    :user/friends ["zhangfei" "guanyu"]}
                                   {:user/name "ganfuren"}
                                   {:user/name "liuchan" :user/relatives [{:relative/user #db/id[:db.part/user -1]
                                                                       :relative/type :relative.type/dad}]}])))

(deftest test-atom-plus
  (let [conn (init-conn sample-def)
        uid [:user/name "liubei"]]
    @(d/transact conn [[:db.fn/atom-plus uid :user/height 30]])
    (is (= (->> (d/entity (d/db conn) uid) :user/height) 166)
        "atom-plus 可以将数字字段进行比较后设置")))

(deftest test-lookup-eid
  (let [conn (init-conn sample-def)
        uid (lookup-eid (d/db conn) [[:user/name "liuchan"] [:user/relatives {:relative/type :relative.type/dad}]])]
    (is (= (->> (d/entity (d/db conn) uid) :relative/user :user/name) "liubei")
        "lookup-eid 使用连续路径来引用 component")))

(deftest test-update-set
  (let [conn (init-conn sample-def)
        uid [:user/name "liubei"]]
    @(d/transact conn [[:db.fn/update-set uid :user/friends ["guanyu" "zhaoyun"]]])
    (is (= (->> (d/entity (d/db conn) uid) :user/friends)
           #{"guanyu" "zhaoyun"})
        "update-set 可以对 set 类型的属性进行更新")))