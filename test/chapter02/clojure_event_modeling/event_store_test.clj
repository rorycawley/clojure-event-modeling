
(ns chapter02.clojure-event-modeling.event-store-test
  "Tests for the event store.

  Tests focus on core concepts:
  - Creating and appending events
  - Validation at boundaries
  - Reading events
  - Events as pure data"
  (:require [clojure.test :refer [deftest testing is]]
            [chapter02.clojure-event-modeling.event-store :as store])
  (:import [java.time Instant]))

;; Test Helpers
;; ============================================================================

(defn make-event
  "Helper to create test events with proper namespaced keywords."
  [stream-id event-type payload]
  {:stream-id stream-id
   :type event-type
   :payload payload
   :metadata {:event/created-at (Instant/now)
              :event/created-by "test"}
   :version 1})

;; Basic Operations
;; ============================================================================

(deftest create-and-append-test
  (testing "Create empty store and append events"
    (let [s (store/create-store)]
      ;; Empty at start
      (is (empty? @s))

      ;; Append first event
      (let [e1 (store/append-event! s
                                    (make-event "stream_a"
                                                :event-1
                                                {:test/data "value"}))]
        ;; ID assigned
        (is (= 0 (:id e1)))
        ;; In store
        (is (= 1 (count @s))))

      ;; IDs increment
      (let [e2 (store/append-event! s
                                    (make-event "stream_a"
                                                :event-2
                                                {}))]
        (is (= 1 (:id e2)))
        (is (= 2 (count @s)))))))

;; Validation
;; ============================================================================

(deftest validation-test
  (testing "Missing required fields fail"
    (let [s (store/create-store)]
      ;; Missing stream-id
      (is (thrown? AssertionError
                   (store/append-event! s
                                        {:type :test
                                         :payload {}
                                         :metadata {}
                                         :version 1})))

      ;; Missing type
      (is (thrown? AssertionError
                   (store/append-event! s
                                        {:stream-id "test"
                                         :payload {}
                                         :metadata {}
                                         :version 1})))

      ;; Missing payload
      (is (thrown? AssertionError
                   (store/append-event! s
                                        {:stream-id "test"
                                         :type :test
                                         :metadata {}
                                         :version 1}))))))

;; Reading Events
;; ============================================================================

(deftest read-all-test
  (testing "Read all events in order"
    (let [s (store/create-store)]
      (store/append-event! s (make-event "stream_a" :event-1 {}))
      (store/append-event! s (make-event "stream_b" :event-2 {}))
      (store/append-event! s (make-event "stream_a" :event-3 {}))

      (let [events (store/read-all-events s)]
        (is (= 3 (count events)))
        (is (= [0 1 2] (map :id events)))))))

(deftest read-stream-test
  (testing "Read events for a specific stream"
    (let [s (store/create-store)]
      (store/append-event! s (make-event "stream_a" :event-1 {}))
      (store/append-event! s (make-event "stream_b" :event-2 {}))
      (store/append-event! s (make-event "stream_a" :event-3 {}))

      (let [events (store/read-stream s "stream_a")]
        ;; Only stream_a events
        (is (= 2 (count events)))
        (is (= [0 2] (map :id events)))
        (is (every? #(= "stream_a" (:stream-id %)) events)))))

  (testing "Non-existent stream returns empty"
    (let [s (store/create-store)
          events (store/read-stream s "stream_xyz")]
      (is (empty? events)))))

(deftest read-by-type-test
  (testing "Read events by type across all streams"
    (let [s (store/create-store)]
      (store/append-event! s (make-event "stream_a" :order-placed {}))
      (store/append-event! s (make-event "stream_a" :payment-processed {}))
      (store/append-event! s (make-event "stream_b" :order-placed {}))

      (let [events (store/read-by-type s :order-placed)]
        ;; Both order-placed events
        (is (= 2 (count events)))
        (is (every? #(= :order-placed (:type %)) events))
        ;; From different streams
        (is (= #{"stream_a" "stream_b"}
               (set (map :stream-id events))))))))

;; Event Structure
;; ============================================================================

(deftest event-structure-test
  (testing "Events have required fields"
    (let [s (store/create-store)
          event (store/append-event! s
                                     (make-event "stream_test"
                                                 :test-event
                                                 {:order/total 99.99}))]
      ;; Has all fields
      (is (contains? event :id))
      (is (contains? event :stream-id))
      (is (contains? event :type))
      (is (contains? event :payload))
      (is (contains? event :metadata))
      (is (contains? event :version))

      ;; Payload preserved
      (is (= 99.99 (get-in event [:payload :order/total]))))))

(deftest namespaced-keywords-test
  (testing "Events use namespaced keywords properly"
    (let [s (store/create-store)
          event (store/append-event! s
                                     {:stream-id "stream_order"
                                      :type :order-placed
                                      :payload {:order/id "order-123"
                                                :order/total 99.99}
                                      :metadata {:event/created-at (Instant/now)}
                                      :version 1})]
      ;; Domain data uses domain namespace
      (is (= "order-123" (get-in event [:payload :order/id])))
      (is (= 99.99 (get-in event [:payload :order/total])))

      ;; Metadata uses :event/ namespace
      (is (contains? (:metadata event) :event/created-at)))))

;; Purity
;; ============================================================================

(deftest read-functions-are-pure-test
  (testing "Read functions don't modify the store"
    (let [s (store/create-store)]
      (store/append-event! s (make-event "stream_a" :event-1 {}))
      (store/append-event! s (make-event "stream_a" :event-2 {}))

      (let [original-count (count @s)]
        ;; Run all reads
        (store/read-all-events s)
        (store/read-stream s "stream_a")
        (store/read-by-type s :event-1)

        ;; Store unchanged
        (is (= original-count (count @s)))))))

(deftest events-as-data-test
  (testing "Events are plain maps"
    (let [s (store/create-store)
          event-data {:stream-id "stream_test"
                      :type :test
                      :payload {:order/amount 50.00}
                      :metadata {:event/created-by "test"}
                      :version 1}
          stored (store/append-event! s event-data)]

      ;; Can use map operations
      (is (map? stored))
      (is (= :test (:type stored)))
      (is (= 50.00 (get-in stored [:payload :order/amount])))

      ;; Original unchanged
      (is (not (contains? event-data :id))))))