(ns earthworm.util.workflow-test
  (:require [clojure.test :refer :all]
            [earthworm.util.workflow :refer :all]
            [clojure.core.async :as async :refer (go go-loop)]))

(def my-flow [[:input (map inc) :output]])

(deftest basic-flow
  (let [chs (workflow my-flow)
        from (get chs :input)
        out (get chs :output)]
    (async/onto-chan from [1 2 3])
    (is (= (chan-content out) [2 3 4]))))