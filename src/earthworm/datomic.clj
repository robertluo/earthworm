(ns earthworm.datomic
  "提供 datomic 数据库服务"
  (:require [datomic.api :as d]
            [earthworm.datomic.util :as util]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log :refer (info)]))

(defrecord DatomicDatabase [uri db-def sample? connection]
  component/Lifecycle
  (start [this]
    (if connection
      this
      (do
        (info {:event :start :component :datomic})
        (assoc this :connection (util/init-conn db-def uri sample?)))))
  (stop [this]
    (if connection
      (do
        (info {:event :stop :component :datomic})
        (when sample?
          (info {:event :delete-database :component :datomic})
          (d/delete-database uri))
        (assoc this :connection nil))
      this)))

(def service map->DatomicDatabase)