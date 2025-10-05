(ns chapter01.clojure-event-modeling.core)

(defn greet
  "Returns a greeting message for the given name."
  [name]
  (str "Hello, " name "!"))

(defn add-numbers
  "Adds two numbers together."
  [a b]
  (+ a b))

(defn -main [& _args]
  (println (greet "World"))
  (println "2 + 3 =" (add-numbers 2 3)))