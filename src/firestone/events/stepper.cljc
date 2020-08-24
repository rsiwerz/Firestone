(ns firestone.events.stepper
  (:require [ysera.test :refer [is is=]]
            [firestone.construct :refer [get-minions
                                         get-secrets
                                         create-game]]
            [firestone.events.queue :refer [qpeek
                                            dequeue
                                            enqueue-event
                                            peek-event
                                            enqueue]]
            [firestone.events.handlers :refer [event-map]]
            [firestone.util :refer [flip-partial
                                    printerr]]))


(defn run-triggers
  [state event]
  (let [f (fn [state entity]
            (let [ret ((:trigger entity) event entity state)]
              (if (some? ret)
                ret
                (do (printerr (str "Warning: "
                                   (:name entity)
                                   "'s trigger returned nil."))
                    state))))]
    (->> (concat
           (get-minions state)
           (get-secrets state))
         (filter (fn [x] (contains? x :trigger)))
         (reduce f state))))

(defn step-event
  {:test (fn []
           (is= (-> (create-game)
                    (enqueue-event :start-of-turn)
                    (step-event)
                    (step-event))
                nil))}
  [state]
  {:pre [(some? state)]}
  (let [event (peek-event state)
        handler (get event-map (:event-type event))]
    (some-> (some->> event
                     (run-triggers state))
            (update :events dequeue)
            (handler event)
            (update-in [:turn :event-log] enqueue event))))

(defn trace-events
  [state]
  {:pre [(some? state)]}
  (cons state
        (if-not (peek-event state)
          nil
          (lazy-seq (let [next (step-event state)]
                      (assert (some? next)
                              (str "Could not step-event\n"
                                   (prn-str (peek-event state))
                                   "in state\n"
                                   (prn-str state)))
                      (trace-events next))))))

(defn step-events
  {:test (fn []
           (is= (-> (create-game)
                    (enqueue-event :start-of-turn)
                    (step-events)
                    (select-keys [:events :turn]))
                {:events [], :turn {:event-log [{:event-type :start-of-turn}]}})
           (is= (-> (create-game)
                    (step-events)
                    (step-events))
                (create-game)))}
  [state]
  (-> (trace-events state)
      (last)))
