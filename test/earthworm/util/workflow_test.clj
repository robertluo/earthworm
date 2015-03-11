(ns earthworm.util.workflow-test
  (:require [clojure.test :refer :all]
            [earthworm.util.workflow :refer :all]
            [clojure.core.async :as async :refer (go go-loop)]))

(def my-flow [[:input (map inc) :output]
              [:output [#(if (even? %) :even :odd) :even :odd]]])

(deftest basic-flow
  (let [chs (workflow my-flow)
        from (get chs :input)]
    (async/onto-chan from [1 2 3])
    (is (= (chan-content (:odd chs)) [3]))
    (is (= (chan-content (:even chs)) [2 4]))))