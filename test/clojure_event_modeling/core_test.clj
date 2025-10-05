(ns clojure-event-modeling.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-event-modeling.core :as core]))

(deftest greet-test
  (testing "greet function"
    (is (= "Hello, Alice!" (core/greet "Alice")))
    (is (= "Hello, Bob!" (core/greet "Bob")))
    (is (= "Hello, World!" (core/greet "World")))))

(deftest add-numbers-test
  (testing "add-numbers function"
    (is (= 5 (core/add-numbers 2 3)))
    (is (= 0 (core/add-numbers -1 1)))
    (is (= 10 (core/add-numbers 7 3)))))