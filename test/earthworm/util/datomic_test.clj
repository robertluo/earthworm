(ns earthworm.util.datomic-test
  (:require [clojure.test :refer :all]
            [earthworm.util.datomic :refer :all]
            [datomic.api :as d]))

(def sample-def
  (def-db (entities :db.part/db
                    [(attr :user/name :db.type/string "user name" :db/unique :db.unique/identity)
                     (attr :user/height :db.type/long "height of user in cm")])
          nil
          (entities :db.part/user [{:user/name "foo" :user/height 136}])))

(deftest test-builtin-fns
  (let [conn (init-conn sample-def)]
    (is (= (:user/height (d/entity (d/db conn) [:user/name "foo"])) 136))))