(ns chapter02.clojure-event-modeling.projection
  "Projections transform an event stream into different views.

  A projection is just a reduce - we fold over events to build a view.
  Different projections of the same events serve different use cases."
  (:import [java.time Instant ZoneOffset]))

;; Projection 1: Customer Order History (for UI display)
;; ============================================================================

(defn customer-order-history
  "Builds a list of orders for a specific customer.

  Returns a sequence of maps suitable for displaying in a table."
  [events customer-id]
  (->> events
       (filter #(= :order-placed (:event/type %)))
       (filter #(= customer-id (get-in % [:event/data :order/customer-id])))
       (map (fn [event]
              {:order-id (:aggregate/id event)
               :amount (get-in event [:event/data :order/total-amount])
               :timestamp (:event/timestamp event)}))))

;; Projection 2: Cancelled Orders in July (for reporting)
;; ============================================================================

(defn cancelled-in-month
  "Counts cancelled orders in a specific month.

  Returns a single number - total cancelled orders."
  [events year month]
  (->> events
       (filter #(= :order-cancelled (:event/type %)))
       (filter (fn [event]
                 (let [ts (:event/timestamp event)
                       ;; Convert Instant to LocalDateTime to get year/month
                       ldt (.atZone ts ZoneOffset/UTC)]
                   (and (= year (.getYear ldt))
                        (= month (.getMonthValue ldt))))))
       count))

;; Projection 3: Orders Per Hour (for real-time dashboard)
;; ============================================================================

(defn orders-per-hour
  "Groups orders by hour for activity tracking.

  Returns a map of hour -> count, like {14 5, 15 8, 16 3}
  meaning 5 orders at 2pm, 8 orders at 3pm, etc."
  [events]
  (->> events
       (filter #(= :order-placed (:event/type %)))
       (map (fn [event]
              (let [ts (:event/timestamp event)]
                ;; Convert Instant to ZonedDateTime to get hour
                (.getHour (.atZone ts ZoneOffset/UTC)))))
       frequencies))

;; Rich Comment - REPL Examples
;; ============================================================================

(comment
  ;; First, let's create some sample events to project
  ;; Note: Instant is already imported at the namespace level

  ;; Helper to create events with timestamps
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

  ;; Sample event stream - what happened in our system
  (def sample-events
    [(make-order-event "order-1" "customer-alice" 99.99 "2025-07-15T14:30:00Z")
     (make-order-event "order-2" "customer-bob" 149.99 "2025-07-15T15:45:00Z")
     (make-order-event "order-3" "customer-alice" 79.99 "2025-07-16T14:20:00Z")
     (make-cancel-event "order-2" "2025-07-16T16:00:00Z")
     (make-order-event "order-4" "customer-alice" 199.99 "2025-07-20T15:10:00Z")
     (make-order-event "order-5" "customer-bob" 299.99 "2025-08-01T10:30:00Z")
     (make-cancel-event "order-5" "2025-08-01T11:00:00Z")])

  ;; Projection 1: Customer Order History
  ;; -------------------------------------
  ;; Use case: Show Alice all her orders in the UI

  (customer-order-history sample-events "customer-alice")
  ;; => ({:order-id "order-1", :amount 99.99, :timestamp #inst "2025-07-15T14:30:00Z"}
  ;;     {:order-id "order-3", :amount 79.99, :timestamp #inst "2025-07-16T14:20:00Z"}
  ;;     {:order-id "order-4", :amount 199.99, :timestamp #inst "2025-07-20T15:10:00Z"})

  ;; Bob's order history
  (customer-order-history sample-events "customer-bob")
  ;; => ({:order-id "order-2", :amount 149.99, :timestamp #inst "2025-07-15T15:45:00Z"}
  ;;     {:order-id "order-5", :amount 299.99, :timestamp #inst "2025-08-01T10:30:00Z"})

  ;; Customer with no orders
  (customer-order-history sample-events "customer-charlie")
  ;; => ()

  ;; Projection 2: Cancelled Orders Report
  ;; --------------------------------------
  ;; Use case: Management wants to know cancellations in July

  (cancelled-in-month sample-events 2025 7)
  ;; => 1
  ;; (Only order-2 was cancelled in July)

  ;; Check August cancellations
  (cancelled-in-month sample-events 2025 8)
  ;; => 1
  ;; (order-5 was cancelled in August)

  ;; No cancellations in June
  (cancelled-in-month sample-events 2025 6)
  ;; => 0

  ;; Projection 3: Orders Per Hour
  ;; ------------------------------
  ;; Use case: Marketing dashboard showing when customers are active

  (orders-per-hour sample-events)
  ;; => {14 2, 15 2, 10 1}
  ;; Translation: 2 orders at 2pm, 2 orders at 3pm, 1 order at 10am

  ;; Same events, three different views!
  ;; ------------------------------------
  ;; The key insight: we're not changing the events, just looking at them
  ;; differently depending on what question we're trying to answer.

  ;; All three projections start with the same event stream
  (count sample-events)
  ;; => 7 events total

  ;; But give us different answers:
  {:alice-orders (count (customer-order-history sample-events "customer-alice"))
   :july-cancellations (cancelled-in-month sample-events 2025 7)
   :busiest-hour (apply max-key val (orders-per-hour sample-events))}
  ;; => {:alice-orders 3, :july-cancellations 1, :busiest-hour [14 2]}

  ;; Combining projections
  ;; ---------------------
  ;; You can build new projections from existing ones

  ;; Total value of Alice's orders
  (->> (customer-order-history sample-events "customer-alice")
       (map :amount)
       (reduce + 0))
  ;; => 379.97

  ;; All customers who placed orders
  (->> sample-events
       (filter #(= :order-placed (:event/type %)))
       (map #(get-in % [:event/data :order/customer-id]))
       distinct)
  ;; => ("customer-alice" "customer-bob")

  ;; Building a simple dashboard
  ;; ---------------------------

  (defn dashboard [events]
    {:total-orders (->> events
                        (filter #(= :order-placed (:event/type %)))
                        count)
     :total-cancelled (->> events
                           (filter #(= :order-cancelled (:event/type %)))
                           count)
     :orders-by-hour (orders-per-hour events)})

  (dashboard sample-events)
  ;; => {:total-orders 5
  ;;     :total-cancelled 2
  ;;     :orders-by-hour {14 2, 15 2, 10 1}}

  ;; Real-time updates
  ;; -----------------
  ;; As new events come in, just re-run the projection

  (def new-order
    (make-order-event "order-6" "customer-alice" 50.00 "2025-08-01T14:30:00Z"))

  (def updated-events (conj sample-events new-order))

  ;; Alice now has 4 orders instead of 3
  (count (customer-order-history updated-events "customer-alice"))
  ;; => 4

  ;; Hour 14 now has 3 orders instead of 2
  (orders-per-hour updated-events)
  ;; => {14 3, 15 2, 10 1}
  )