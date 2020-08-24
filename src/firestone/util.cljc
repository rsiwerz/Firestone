(ns firestone.util
  (:require [ysera.test :refer [is is-not is= error?]]
            [clojure.pprint :refer [pprint]]))

(defn itertimes
  [f n x] (nth (iterate f x) n))

(defn irange [a b]
  "Inclusive range from a to b. If b<a the range is decreasing."
  (if (<= a b)
    (take (inc (- b a)) (iterate inc a))
    (take (inc (- a b)) (iterate dec a))))

(defn trace
  "Prints the argument and returns it."
  [x]
  (pprint x)
  x)

(defn printerr
  "Print the argument to STDERR"
  [x]
  (binding [*out* *err*]
    (println x)))

(defn not-nil?
  "DEPRECATED: Use 'some?' instead.
   Aparantly called 'some?' in clojure"
  {:private true
   :deprecated true}
  [x] (some? x))

(defn flip-partial
  [f & args]
  (fn [x]
    (apply f x args)))