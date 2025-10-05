(ns chapter02.clojure-event-modeling.event-store
  "Simple in-memory event store for learning event sourcing.

  Events are stored in order. Each event belongs to a stream.

  Book principles applied:
  - Events as pure data (maps)
  - Validation at boundaries
  - Operational (append!) vs functional (read-) separation
  - Namespaced keywords for clarity"
  (:import [java.time Instant]))

;; Store Creation
;; ============================================================================

(defn create-store
  "Creates an empty event store.

  Returns an atom containing a vector. Vector index = event ID."
  []
  (atom []))

;; Operational - Writing (has side effects)
;; ============================================================================

(defn append-event!
  "Appends an event to the store.

  Event map must contain:
  - :stream-id (which stream)
  - :type (what happened, past tense like :order-placed)
  - :payload (domain data with namespaced keywords)
  - :metadata (event info with :event/ namespace)
  - :version (integer)

  Returns the event with :id added."
  [store event]
  {:pre [(:stream-id event)
         (:type event)
         (contains? event :payload)
         (contains? event :metadata)
         (contains? event :version)]}
  (let [event-id (count @store)
        event-with-id (assoc event :id event-id)]
    (swap! store conj event-with-id)
    event-with-id))

;; Functional - Reading (pure functions)
;; ============================================================================

(defn read-all-events
  "Returns all events in order."
  [store]
  @store)

(defn read-stream
  "Returns all events for a stream in order."
  [store stream-id]
  (filter #(= stream-id (:stream-id %)) @store))

(defn read-by-type
  "Returns all events of a type across all streams."
  [store event-type]
  (filter #(= event-type (:type %)) @store))

;; Rich Comment - Learning Examples
;; ============================================================================

(comment
  ;; Create store
  (def store (create-store))

  (read-all-events store)
  ;; => []

  ;; Append first event
  ;; Note: namespaced keywords! :order/ for order data, :event/ for metadata
  (append-event! store
                 {:stream-id "stream_customer_47"
                  :type :order-placed
                  :payload {:order/id "order-123"
                            :order/customer-id "customer-47"
                            :order/total 99.99}
                  :metadata {:event/created-at (Instant/now)
                             :event/created-by "web-api"}
                  :version 1})
  ;; => {:id 0
  ;;     :stream-id "stream_customer_47"
  ;;     :type :order-placed
  ;;     :payload {...}
  ;;     :metadata {...}
  ;;     :version 1}

  ;; Append second event (same stream)
  (append-event! store
                 {:stream-id "stream_customer_47"
                  :type :payment-processed
                  :payload {:payment/order-id "order-123"
                            :payment/amount 99.99}
                  :metadata {:event/created-at (Instant/now)
                             :event/created-by "payment-service"}
                  :version 1})

  ;; Append event for different customer
  (append-event! store
                 {:stream-id "stream_customer_48"
                  :type :order-placed
                  :payload {:order/id "order-124"
                            :order/customer-id "customer-48"
                            :order/total 149.99}
                  :metadata {:event/created-at (Instant/now)
                             :event/created-by "web-api"}
                  :version 1})

  ;; Read all events
  (read-all-events store)
  ;; => [{:id 0 ...} {:id 1 ...} {:id 2 ...}]

  ;; Read one customer's stream
  (read-stream store "stream_customer_47")
  ;; => [{:id 0 :type :order-placed ...}
  ;;     {:id 1 :type :payment-processed ...}]

  ;; Read all order-placed events
  (read-by-type store :order-placed)
  ;; => [{:id 0 :stream-id "stream_customer_47" ...}
  ;;     {:id 2 :stream-id "stream_customer_48" ...}]

  ;; Access domain data using namespaced keywords
  (let [events (read-stream store "stream_customer_47")
        first-event (first events)]
    (get-in first-event [:payload :order/total]))
  ;; => 99.99

  ;; Validation at boundary - missing field fails
  (append-event! store {:stream-id "test" :type :test})
  ;; => AssertionError: Assert failed: (contains? event :payload)

  ;; Helper for less boilerplate
  (defn make-event [stream-id type payload-data]
    {:stream-id stream-id
     :type type
     :payload payload-data
     :metadata {:event/created-at (Instant/now)
                :event/created-by "my-service"}
     :version 1})

  (append-event! store
                 (make-event "stream_customer_49"
                             :order-placed
                             {:order/id "order-125"
                              :order/customer-id "customer-49"
                              :order/total 199.99}))

  ;; Build a simple projection
  (defn total-orders-per-customer [store]
    (->> (read-by-type store :order-placed)
         (map #(get-in % [:payload :order/customer-id]))
         frequencies))

  (total-orders-per-customer store)
  ;; => {"customer-47" 1, "customer-48" 1, "customer-49" 1}

  ;; Event versioning - schema evolution
  (append-event! store
                 {:stream-id "stream_customer_50"
                  :type :order-placed
                  :payload {:order/id "order-126"
                            :order/customer-id "customer-50"
                            :order/total 299.99
                            :order/items [{:product/id "p1" :qty 2}]} ; v2 adds items
                  :metadata {:event/created-at (Instant/now)}
                  :version 2})  ; version 2

  ;; Why namespaced keywords?
  ;; Without: is :id an order-id, customer-id, or product-id?
  ;; With: :order/id vs :customer/id vs :product/id - crystal clear!
  )