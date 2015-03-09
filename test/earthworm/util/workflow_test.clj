(ns earthworm.util.workflow-test
  (:require [clojure.test :refer :all]
            [earthworm.util.workflow :refer :all]
            [clojure.core.async :as async :refer (go go-loop)]))

(defn tokenize [s]
  (clojure.string/split s #"\s+"))

(def my-flow [[:sentence tokenize :words]])

(comment
  (deftest basic-flow
    (let [chs (workflow my-flow)
          from (get chs :sentence)
          out (get chs :words)]
      (async/onto-chan from ["hello world"])
      (is (= (chan-content out) ["hello" "world"])))))