(defproject earthworm "0.2.1"
  :description "Common components and utils for server side development"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.2.2"]
                 [com.stuartsierra/component "0.2.2"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.datomic/datomic-pro "0.9.5067" :exclusions [joda-time]]
                 [http-kit "2.1.18"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"])