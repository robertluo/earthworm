(ns earthworm.http-kit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as kit]
            [taoensso.timbre :refer (info)]))

(defrecord HttpKitServer [routes options server-stop]
  component/Lifecycle
  (start [this]
    (info {:event :start :component :http-kit})
    (if-let [fn-stop (kit/run-server (:handler routes) options)]
      (assoc this :server-stop fn-stop)
      (throw (ex-info "Unable to run http-kit server" {:reason "run-server nil"}))))
  (stop [this]
    (when server-stop
      (info {:event :stop :component :http-kit})
      (server-stop)
      (assoc this :server-stop nil :routes nil))))

(defn service [config-options]
  (map->HttpKitServer {:options (merge {:port 8080 :queue-size 10000} config-options)}))