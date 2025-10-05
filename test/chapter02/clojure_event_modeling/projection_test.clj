(ns chapter02.clojure-event-modeling.projection-test
  "Tests for event projections."
  (:require [clojure.test :refer [deftest testing is]]
            [chapter02.clojure-event-modeling.projection :as proj])
  (:import [java.time Instant]))

;; Test Data
;; ============================================================================

(defn make-order-event [order-id customer-id amount timestamp-str]
  {:event/type :order-placed
   :event/id (random-uuid)
   :event/timestamp (Instant/parse timestamp-str)
   :aggregate/id order-id
   :aggregate/type :order
   :event/data {:order/customer-id customer-id
                :order/total-amount amount}})

(defn make-cancel-event [order-id timestamp-str]
  {:event/type :order-cancelled
   :event/id (random-uuid)
   :event/timestamp (Instant/parse timestamp-str)
   :aggregate/id order-id
   :aggregate/type :order
   :event/data {}})

(def test-events
  [(make-order-event "order-1" "alice" 99.99 "2025-07-15T14:30:00Z")
   (make-order-event "order-2" "bob" 149.99 "2025-07-15T15:45:00Z")
   (make-order-event "order-3" "alice" 79.99 "2025-07-16T14:20:00Z")
   (make-cancel-event "order-2" "2025-07-16T16:00:00Z")
   (make-order-event "order-4" "alice" 199.99 "2025-07-20T15:10:00Z")])

;; Tests
;; ============================================================================

(deftest customer-order-history-test
  (testing "Projects order history for a specific customer"
    (let [history (proj/customer-order-history test-events "alice")]

      ;; Alice has 3 orders
      (is (= 3 (count history)))

      ;; Each order has the expected shape
      (is (every? #(contains? % :order-id) history))
      (is (every? #(contains? % :amount) history))
      (is (every? #(contains? % :timestamp) history))

      ;; First order matches
      (is (= "order-1" (:order-id (first history))))
      (is (= 99.99 (:amount (first history))))))

  (testing "Returns empty for customer with no orders"
    (let [history (proj/customer-order-history test-events "charlie")]
      (is (empty? history)))))

(deftest cancelled-in-month-test
  (testing "Counts cancelled orders in July 2025"
    ;; One cancellation in July
    (is (= 1 (proj/cancelled-in-month test-events 2025 7))))

  (testing "Returns zero for months with no cancellations"
    (is (= 0 (proj/cancelled-in-month test-events 2025 6)))
    (is (= 0 (proj/cancelled-in-month test-events 2025 8)))))

(deftest orders-per-hour-test
  (testing "Groups orders by hour of day"
    (let [by-hour (proj/orders-per-hour test-events)]

      ;; Hour 14 (2pm) has 2 orders
      (is (= 2 (get by-hour 14)))

      ;; Hour 15 (3pm) has 2 orders
      (is (= 2 (get by-hour 15)))

      ;; Hours with no orders aren't in the map
      (is (nil? (get by-hour 12))))))

(deftest projections-are-pure-test
  (testing "Running projections doesn't modify events"
    (let [original-count (count test-events)]

      ;; Run all projections
      (proj/customer-order-history test-events "alice")
      (proj/cancelled-in-month test-events 2025 7)
      (proj/orders-per-hour test-events)

      ;; Events unchanged
      (is (= original-count (count test-events))))))