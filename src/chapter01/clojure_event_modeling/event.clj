(ns chapter01.clojure-event-modeling.event
  "Simple event creation for event sourcing.

  Events are pure data - plain maps describing what happened.")

(defn create-event
  "Creates an event with required fields and optional data.

  Required:
  - event-type: keyword in past tense (e.g., :order-placed)
  - aggregate-id: which entity changed
  - aggregate-type: type of entity (e.g., :order)
  - data: map of domain-specific data

  Returns a plain map - pure data, no behavior."
  [event-type aggregate-id aggregate-type data]
  {:event/type event-type
   :aggregate/id aggregate-id
   :aggregate/type aggregate-type
   :event/data data})

(defn order-placed
  "Creates an order-placed event.

  Past-tense naming makes it clear this describes something that happened."
  [order-id customer-id total-amount]
  (create-event :order-placed
                order-id
                :order
                {:order/customer-id customer-id
                 :order/total-amount total-amount}))

;; Rich Comment Block - REPL Examples
;; ============================================================================
;; Evaluate these expressions in your REPL to see how events work.
;; Place your cursor after each form and press your editor's eval command.

(comment
  ;; Basic event creation
  ;; --------------------

  ;; Create a simple event with the minimum required fields
  (create-event :order-placed
                "order-123"
                :order
                {:order/customer-id "customer-456"
                 :order/total-amount 99.99})
  ;; => {:event/type :order-placed
  ;;     :aggregate/id "order-123"
  ;;     :aggregate/type :order
  ;;     :event/data {:order/customer-id "customer-456"
  ;;                  :order/total-amount 99.99}}

  ;; Events are just maps, so we can bind them to names
  (def my-event
    (create-event :order-placed
                  "order-123"
                  :order
                  {:order/customer-id "customer-456"
                   :order/total-amount 99.99}))

  ;; Access fields like any map
  (:event/type my-event)
  ;; => :order-placed

  (:aggregate/id my-event)
  ;; => "order-123"

  ;; Access nested data
  (get-in my-event [:event/data :order/customer-id])
  ;; => "customer-456"

  ;; Or use threading
  (-> my-event :event/data :order/total-amount)
  ;; => 99.99

  ;; Different event types
  ;; ---------------------

  ;; Payment event - different aggregate, different data
  (create-event :payment-processed
                "order-123"
                :order
                {:payment/method :credit-card
                 :payment/amount 99.99
                 :payment/transaction-id "txn-abc123"})
  ;; => {:event/type :payment-processed
  ;;     :aggregate/id "order-123"
  ;;     :aggregate/type :order
  ;;     :event/data {:payment/method :credit-card
  ;;                  :payment/amount 99.99
  ;;                  :payment/transaction-id "txn-abc123"}}

  ;; Product event - different aggregate type entirely
  (create-event :inventory-depleted
                "product-789"
                :product
                {:inventory/current-quantity 5
                 :inventory/threshold 10})
  ;; => {:event/type :inventory-depleted
  ;;     :aggregate/id "product-789"
  ;;     :aggregate/type :product
  ;;     :event/data {:inventory/current-quantity 5
  ;;                  :inventory/threshold 10}}

  ;; Using domain-specific constructors
  ;; -----------------------------------

  ;; The order-placed helper makes it cleaner
  (order-placed "order-123" "customer-456" 99.99)
  ;; => {:event/type :order-placed
  ;;     :aggregate/id "order-123"
  ;;     :aggregate/type :order
  ;;     :event/data {:order/customer-id "customer-456"
  ;;                  :order/total-amount 99.99}}

  ;; Compare with the manual version - same result, less boilerplate
  (create-event :order-placed
                "order-123"
                :order
                {:order/customer-id "customer-456"
                 :order/total-amount 99.99})

  ;; Multiple events for the same order
  (def order-id "order-123")

  (def placed-event
    (order-placed order-id "customer-456" 99.99))

  (def payment-event
    (create-event :payment-processed
                  order-id
                  :order
                  {:payment/method :credit-card
                   :payment/amount 99.99}))

  (def shipped-event
    (create-event :order-shipped
                  order-id
                  :order
                  {:shipping/carrier "FedEx"
                   :shipping/tracking-number "1Z999AA1"}))

  ;; All events share the same aggregate-id
  (:aggregate/id placed-event)   ;; => "order-123"
  (:aggregate/id payment-event)  ;; => "order-123"
  (:aggregate/id shipped-event)  ;; => "order-123"

  ;; Working with collections of events
  ;; -----------------------------------

  ;; Create a sequence of events
  (def events
    [(order-placed "order-123" "customer-456" 99.99)
     (create-event :payment-processed
                   "order-123"
                   :order
                   {:payment/method :credit-card})
     (create-event :order-shipped
                   "order-123"
                   :order
                   {:shipping/carrier "FedEx"})])

  ;; Get all event types
  (map :event/type events)
  ;; => (:order-placed :payment-processed :order-shipped)

  ;; Filter by event type
  (filter #(= :order-placed (:event/type %)) events)

  ;; Find events for a specific order
  (filter #(= "order-123" (:aggregate/id %)) events)

  ;; Group events by aggregate
  (group-by :aggregate/id events)
  ;; => {"order-123" [{:event/type :order-placed ...} {...} {...}]}

  ;; Events as pure data
  ;; -------------------

  ;; Events can be printed and read back
  (def event-string
    (pr-str (order-placed "order-123" "customer-456" 99.99)))

  event-string
  ;; => "{:event/type :order-placed, :aggregate/id \"order-123\", ...}"

  (def parsed-event (read-string event-string))

  ;; They're identical
  (= (order-placed "order-123" "customer-456" 99.99)
     parsed-event)
  ;; => true

  ;; Events can be merged with additional data
  (merge (order-placed "order-123" "customer-456" 99.99)
         {:event/id (random-uuid)
          :event/timestamp (java.time.Instant/now)})
  ;; => {:event/type :order-placed
  ;;     :aggregate/id "order-123"
  ;;     :aggregate/type :order
  ;;     :event/data {:order/customer-id "customer-456"
  ;;                  :order/total-amount 99.99}
  ;;     :event/id #uuid "..."
  ;;     :event/timestamp #inst "..."}

  ;; Common patterns
  ;; ---------------

  ;; Creating an event from a map of attributes
  (let [order-attrs {:id "order-123"
                     :customer-id "customer-456"
                     :total 99.99}]
    (order-placed (:id order-attrs)
                  (:customer-id order-attrs)
                  (:total order-attrs)))

  ;; Validating event structure
  (let [event (order-placed "order-123" "customer-456" 99.99)]
    (and (contains? event :event/type)
         (contains? event :aggregate/id)
         (contains? event :aggregate/type)
         (contains? event :event/data)))
  ;; => true

  ;; Extracting just the domain data
  (:event/data (order-placed "order-123" "customer-456" 99.99))
  ;; => {:order/customer-id "customer-456", :order/total-amount 99.99}

  ;; Building a simple event stream
  (defn simulate-order-flow [order-id customer-id amount]
    [(order-placed order-id customer-id amount)
     (create-event :payment-processed
                   order-id
                   :order
                   {:payment/method :credit-card
                    :payment/amount amount})
     (create-event :order-shipped
                   order-id
                   :order
                   {:shipping/carrier "FedEx"})])

  (simulate-order-flow "order-999" "customer-111" 149.99)
  ;; => [{:event/type :order-placed ...}
  ;;     {:event/type :payment-processed ...}
  ;;     {:event/type :order-shipped ...}]

  ;; Note: In production, you would add :event/id and :event/timestamp
  ;; at the boundary when persisting to the event store
  )