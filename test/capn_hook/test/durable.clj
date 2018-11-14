(ns capn-hook.test.durable
  "Namespace for simple unit tests on the functions in the durable namespace."
  (:require [clj-time.core :refer [now]]
            [clojure.test :refer :all]
            [capn-hook.durable :refer :all]))

(deftest post-test
  (is (post {:created (now) :url "https://postman-echo.com/post" :msg {:a 5 :b 6 :c 7}}))
  (is (not (post {:created (now) :url "" :msg {:a 5 :b 6 :c 7}}))) )

