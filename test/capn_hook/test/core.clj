(ns capn-hook.test.core
  "Namespace for simple unit tests on the functions in the core namespace."
  (:require [clojure.test :refer :all]
            [capn-hook.core :refer :all]))

(deftest register-test
  (are [url, ans] (= ans (register! :foo url))
    "bobby" {:foo #{"bobby"}}
    "bobby" {:foo #{"bobby"}}
    "teddy" {:foo #{"bobby" "teddy"}}
    "al"    {:foo #{"bobby" "teddy" "al"}}
    "teddy" {:foo #{"bobby" "teddy" "al"}} ))

(deftest deregister-test
  (reset! registrations {:foo #{"bobby", "teddy", "al"} :bar #{"a", "b", "c"}})
  (are [url, ans] (= ans (deregister! url))
    "bobby" {:foo #{"teddy", "al"} :bar #{"a", "b", "c"}}
    "bobby" {:foo #{"teddy", "al"} :bar #{"a", "b", "c"}}
    "teddy" {:foo #{"al"} :bar #{"a", "b", "c"}}
    "al"    {:foo #{} :bar #{"a", "b", "c"}}
    "teddy" {:foo #{} :bar #{"a", "b", "c"}} ))
