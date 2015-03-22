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

(defn- get-ch [chs name]
  (if-let [ch (get @chs name)]
    ch
    (do
      (swap! chs assoc name (async/chan (async/sliding-buffer 1024)))
      (get @chs name))))

(defn workflow
  [pl-def]
  (let [error-chan (async/chan 20)
        channels (atom {:exception error-chan})
        ex-handler (fn [e] [:error e])]
    (doseq [[from xf to] pl-def]
      (if (vector? xf)
        ; 当第二参数是 vector 时, 分离通道
        (let [from (get-ch channels from)
              [split-fn & dests] xf
              pub (async/pub from split-fn)]
          (doseq [topic dests]
            (async/sub pub topic (get-ch channels topic))))
        ; 流水线定义
        (let [from (get-ch channels from)
              to (get-ch channels to)]
          (async/pipeline 8 to xf from true ex-handler))))
    @channels))