(ns chapter01.clojure-event-modeling.event-test
  "Tests for simple event creation."
  (:require [clojure.test :refer [deftest testing is]]
            [chapter01.clojure-event-modeling.event :as event]))

(deftest create-event-test
  (testing "Creating a basic event with required fields"
    (let [event (event/create-event :order-placed
                                    "order-123"
                                    :order
                                    {:order/customer-id "customer-456"
                                     :order/total-amount 99.99})]

                       ;; Event is just a map - pure data
      (is (map? event))

                       ;; Has required fields
      (is (= :order-placed (:event/type event)))
      (is (= "order-123" (:aggregate/id event)))
      (is (= :order (:aggregate/type event)))

                       ;; Contains the data we passed in
      (is (= {:order/customer-id "customer-456"
              :order/total-amount 99.99}
             (:event/data event))))))

(deftest order-placed-test
  (testing "Creating an order-placed event"
    (let [event (event/order-placed "order-123"
                                    "customer-456"
                                    99.99)]

                       ;; Correct event type (past tense)
      (is (= :order-placed (:event/type event)))

                       ;; Correct aggregate
      (is (= "order-123" (:aggregate/id event)))
      (is (= :order (:aggregate/type event)))

                       ;; Domain data uses namespaced keywords
      (is (= "customer-456" (get-in event [:event/data :order/customer-id])))
      (is (= 99.99 (get-in event [:event/data :order/total-amount]))))))

(deftest events-are-immutable-test
  (testing "Events are plain data and immutable"
    (let [event (event/order-placed "order-123" "customer-456" 99.99)]

                       ;; Can use standard map functions
      (is (contains? event :event/type))
      (is (= :order-placed (get event :event/type)))

                       ;; Can be serialized to EDN and back
      (let [edn-string (pr-str event)
            parsed-event (read-string edn-string)]
        (is (= event parsed-event))))))