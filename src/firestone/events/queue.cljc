(ns firestone.events.queue
  (:require [ysera.test :refer [is is-not is= error?]]
            [firestone.util :refer [flip-partial
                                    printerr]]))

(defn queue?
  "Tests of the argument is a queue."
  {:test (fn []
           (is= (queue? [1 2 {:front (list 1) :back [2]}])
                false)
           (is= (queue? {:front (list 1) :back [2]})
                true)
           (is= (queue? {:front [1] :back [2]})
                false)
           (is= (queue? {:front [1] :back (list 2)})
                false)
           (is= (queue? {:front (list 1) :back (list 2)})
                false)
           (is= (queue? {:front (list 1 2) :back [3 4]})
                true))}
  [qs]
  (and (map? qs)
       (= (sort (keys qs))
          [:back :front])
       (seq? (:front qs))
       (vector? (:back qs))))

(defn queue
  "Returns a queue with a front and a back.
   (queue) returns an empty queue.
   (queue xs) converts xs to a queue.
   (queue xs ys) returns a queue with xs as front and ys as back."
  {:test (fn []
           (is= (->> (queue)
                     (vals)
                     (map empty?))
                [true true])
           (is= (queue (list 1 2) [3 4])
                {:front (list 1 2) :back [3 4]})
           (is= (:back (queue [1 2 3]))
                [1 2 3])
           (is= (:front (queue (list 1 2 3)))
                (list 1 2 3))
           (is= (:front (queue [1 2 3]))
                (list))
           (is= (:back (queue (list 1 2 3)))
                []))}
  ([]
   (queue (list) []))
  ([xs]
   {:pre [(or (queue? xs)
              (seq? xs)
              (vector? xs))]}
   (if (vector? xs)
     (queue (list) xs)
     (if (seq? xs)
       (queue xs [])
       (if (queue? xs)
         xs
         (queue (list xs) [])))))
  ([front back]
   {:pre [(seq? front) (vector? back)]
    :post [(partial queue?)]}
   {:front front :back back}))

(defn qflatten
  "Converts the argument to a list."
  {:test (fn []
           (is= (qflatten [5 6])
                [5 6]))}
  [xs]
  {:pre [(or (queue? xs)
             (seq? xs)
             (vector? xs)
             (nil? xs))]}
  (if (nil? xs)
    (printerr "Warning: (qflatten nil)"))
  (if (queue? xs)
    (concat (:front xs) (:back xs))
    xs))

(defn qpeek
  "Returns the first item in the queue.
   Returns nil if argument is empty."
  {:test (fn []
           (is= (qpeek (queue [1]))
                1)
           (is= (qpeek (queue [1 2]))
                1)
           (is= (qpeek (queue (list 1)))
                1)
           (is= (qpeek (queue (list 1 2)))
                1)
           (is= (qpeek (queue '(1) [2]))
                1)
           (is= (qpeek nil)
                nil))}
  [xs]
  (if (nil? xs)
    xs
    (first (qflatten xs))))

(defn enqueue
  {:test (fn []
           (is= (enqueue (queue (conj [] 1 2)) 3)
                {:front [] :back [1 2 3]})
           (is= (enqueue (queue (conj (list) 1 2)) 3)
                {:front (list 2 1) :back (list 3)})
           (is= (enqueue (list 1 2 3) 4)
                (queue (list 1 2 3) [4]))
           (is= (enqueue [1 2 3] 4)
                [1 2 3 4])
           (is= (-> (enqueue nil 1)
                    (qflatten))
                [1]))}
  [xs x]
  {:post [(fn [q] (not (empty? q)))]}
  (if (empty? xs)
    [x]
    (if (vector? xs)
      (conj xs x)
      (update (queue xs) :back conj x))))

(defn qpush
  {:test (fn []
           (is= (qpush (queue (conj [] 1 2)) 3)
                {:front [3] :back [1 2]})
           (is= (qpush (queue (conj (list) 1 2)) 3)
                {:front (list 3 2 1) :back (list)})
           (is= (qpush (list 1 2 3) 4)
                (list 4 1 2 3))
           (is= (qpush [1 2 3] 4)
                (queue (list 4) [1 2 3])))}
  [xs x]
  {:post [(fn [q] (not (empty? q)))]}
  (if (empty? xs)
    [x]
    (if (seq? xs)
      (conj xs x)
      (update (queue xs) :front conj x))))


(defn dequeue
  "Removes the last item from a queue."
  {:test (fn []
           (let [qs (queue (list 1 2 3) [4 5 6])]
             (is= (-> qs
                      (qpush 10)
                      (dequeue))
                  qs)
             (is= (-> qs
                      (dequeue)
                      (dequeue)
                      (dequeue)
                      (dequeue))
                  [5 6])
             (is= (-> qs
                      (dequeue)
                      (dequeue)
                      (dequeue)
                      (dequeue)
                      (dequeue)
                      (dequeue)
                      (qflatten))
                  (empty (list))))
           (is= (-> (queue '(1) [2])
                    (dequeue)
                    (dequeue)
                    (dequeue)
                    (qpeek))
                nil)
           (is= (-> {:front (list {:event-type  :summon-minion
                                   :player-id   "p2"
                                   :minion-name "Snake"}
                                  {:event-type  :summon-minion
                                   :player-id   "p2"
                                   :minion-name "Snake"}
                                  {:event-type  :summon-minion
                                   :player-id   "p2"
                                   :minion-name "Snake"})
                     :back  [{:event-type :damage-minion
                              :minion-id  2
                              :damage     1}
                             {:event-type :damage-minion
                              :minion-id  1
                              :damage     7}]}
                    (dequeue)
                    (qflatten))
                [{:event-type  :summon-minion
                  :player-id   "p2"
                  :minion-name "Snake"}
                 {:event-type  :summon-minion
                  :player-id   "p2"
                  :minion-name "Snake"}
                 {:event-type :damage-minion
                  :minion-id  2
                  :damage     1}
                 {:event-type :damage-minion
                  :minion-id  1
                  :damage     7}]))}
  [xs]
  {:pre  [(or (queue? xs)
              (seq? xs)
              (vector? xs))]
   :post [(fn [ret] (or (queue? ret)
                        (seq? ret)
                        (vector? ret)))]}
  (if (queue? xs)
    (if (empty? (:front xs))
      (rest (:back xs))
      (update xs :front rest))
    (rest xs)))


(def get-events
  (comp qflatten :events))

(defn peek-event
  [state]
  (some-> state
          :events
          (qpeek)))

(def third (comp first next next))

(def fourth (comp first next next next))


(defn enqueue-event
  "Put an event last in the event queue in the state.
   The argument can be a event map or the values to put
   in the event"
  {:test (fn []
           (is= (enqueue-event {} :draw-card "p1")
                {:events [{:event-type :draw-card
                           :player-id  "p1"}]})
           (is= (enqueue-event {}
                               :play-card
                               "p1"
                               {:name      "Snake"
                                :card-type :minion})
                {:events [{:event-type :play-card
                           :card-type  :minion
                           :player-id  "p1"
                           :card       {:name      "Snake"
                                        :card-type :minion}}]})
           (is= (enqueue-event {}
                               :play-card
                               "p1"
                               {:name      "Snake"
                                :card-type :minion}
                               0)
                {:events [{:event-type :play-card
                           :card-type  :minion
                           :player-id  "p1"
                           :card       {:name      "Snake"
                                        :card-type :minion}
                           :position   0}]})
           (is= (enqueue-event {}
                               :play-card
                               "p1"
                               {:name      "Snake Trap"
                                :card-type :spell
                                :secret    true})
                {:events [{:event-type :play-card
                           :card-type  :secret
                           :player-id  "p1"
                           :card       {:name      "Snake Trap"
                                        :card-type :spell
                                        :secret    true}}]})
           (is= (enqueue-event {}
                               :end-turn)
                {:events [{:event-type :end-turn}]}))}
  ([state event]
   (if (or (= (:event-type event)
              :end-turn)
           (= (:event-type event)
              :start-of-turn))
     (assert (not (contains? event :player-id))
             "Please use :current-player or :next-player"))
   (if (keyword? event)
     (enqueue-event state {:event-type event})
     (update state :events (fn [events] (enqueue events event)))))
  ([state type & args]
   {:post [(fn [ret] (->> (get-events ret)
                          (last)
                          (vals)
                          (every? some?)))]}

   (enqueue-event state
                  (case type
                    :end-turn       (cond-> {:event-type :end-turn}
                                      (first args) (assoc :current-player (first args)))
                    :start-of-turn  (cond-> {:event-type :start-of-turn}
                                      (first args) (assoc :next-player (first args)))
                    :draw-card {:event-type :draw-card
                                :player-id  (first args)}
                    :damage-minion {:event-type :damage-minion
                                    :minion-id  (first args)
                                    :damage     (second args)}
                    :summon-minion (cond->
                                     {:event-type :summon-minion
                                      :player-id  (first args)}
                                     (string? (second args)) (assoc :minion-name (second args))
                                     (map? (second args))    (assoc :minion (second args))
                                     (third args)            (assoc :position (third args)))
                    :destroy-minion {:event-type :destroy-minion
                                     :player-id  (first args)
                                     :minion-id  (second args)}
                    :attack-minion {:event-type  :attack-minion
                                    :attacker-id (first args)
                                    :target-id   (second args)}
                    :attack-hero {:event-type  :attack-hero
                                  :attacker-id (first args)
                                  :target-id   (second args)}
                    :damage-hero {:event-type :damage-hero
                                  :player-id      (first args)
                                  :damage       (second args)}
                    :play-card (let [id (first args)
                                     card (dissoc (second args)
                                                  :trigger
                                                  :battlecry
                                                  :deathrattle
                                                  :valid-targets
                                                  :spells :spell)
                                     position (third args)]
                                 (assert (:card-type card))
                                 (cond-> {:event-type :play-card
                                          :card-type  (:card-type card)}
                                         id (assoc :player-id id)
                                         card (assoc :card card)
                                         position (assoc :position position)
                                         (fourth args) (assoc :target-id (fourth args))
                                         (:secret card) (assoc :card-type :secret)))
                    :restore-health {:event-type    :restore-health
                                     :character-id  (first  args)
                                     :health        (second args)}))))
