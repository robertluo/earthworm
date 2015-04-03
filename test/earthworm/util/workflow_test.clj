(ns earthworm.util.workflow-test
  (:require [clojure.test :refer :all]
            [earthworm.util.workflow :refer :all]
            [clojure.core.match :refer (match)]
            [clojure.core.async :as async :refer (go go-loop)]))

(def my-flow [[:input (map inc) :output]
              [:output [#(if (even? %) :even :odd) :even :odd]]])

(deftest basic-flow
  (let [chs (workflow my-flow)
        from (get chs :input)]
    (async/onto-chan from [1 2 3])
    (is (= (chan-content (:odd chs)) [3]))
    (is (= (chan-content (:even chs)) [2 4]))))

(defmacro matches
  "用 core.match 的 exp 来检查 value 是否是期待值"
  ;;TODO 移到 earthworm 库
  ;;TODO 失败时可以返回值，而不是报告 false
  [value exp]
  `(is (match ~value
             ~exp true
             :else false)))

(deftest exception-handling
  (let [chs (workflow [[:input (map #(/ 8 %)) :output]])]
    (async/onto-chan (:input chs) [4 2 0 1])
    (matches (chan-content (:output chs)) [2 4 [:error _] 8])))