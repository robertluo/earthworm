(ns earthworm.util.workflow
  "用 dag (有向非循环图) 来定义消息通道之间的工作流"
  (:require [clojure.core.async :as async]))

(defn chan-content
  "在当前线程返回 ch 的内容, 是一个 collection, ch 必须先关闭! 一般用于测试"
  [ch]
  (loop [rst []]
    (if-let [word (async/<!! ch)]
      (recur (conj rst word))
      rst)))

(defn get-ch [chs name]
  (if-let [ch (get @chs name)]
    ch
    (do
      (swap! chs assoc name (async/chan (async/sliding-buffer 1024)))
      (get @chs name))))

(defn workflow
  [pl-def]
  (let [channels (atom {})]
    (doseq [[from xf to] pl-def]
      (if (vector? xf)
        (let [from (get-ch channels from)
              pub (async/pub from first)]
          (doseq [topic xf]
            (async/sub pub topic (get-ch channels topic))))
        (let [from (get-ch channels from)
              to (get-ch channels to)]
          (async/pipeline 8 to xf from))))
    @channels))