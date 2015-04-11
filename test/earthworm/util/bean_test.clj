(ns earthworm.util.bean-test
  (:require [clojure.test :refer :all]
            [earthworm.util.bean :refer :all])
  (:import (java.util Date)))

(deftest rm-nil-test
  (are [src expect] (= (pack (rm-nils src)) expect)
    {:a nil} nil
    {:a 1 :b nil} {:a 1}
    {:a 1 :b {} :c false :d true} {:a 1 :d true}))

(def inc-year #(+ % 1900))
(def dec-year #(- % 1900))
(defbean date Date
         [[:date/month .getMonth .setMonth]
          [:date/year [.getYear inc-year] [.setYear dec-year]]])

(set! *warn-on-reflection* true)

(deftest defbean-test
  (is (= (from-date (Date. 0)) {:date/year 1970}))
  (is (= (from-date (to-date {:date/year 2015 :date/month 2}))) {:date/year 2015 :date/month 2}))