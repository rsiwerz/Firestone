(ns firestone.events
  (:require [ysera.test :refer :all]
            [firestone.events.stepper :refer :all]
            [firestone.actions :refer [init-game
                                       play-spell-card]]
            [firestone.construct :refer [create-game
                                         create-minion
                                         create-card
                                         get-minion
                                         get-minions
                                         get-secrets]]
            [firestone.definitions :refer [get-definition]]
            [firestone.events.triggers :refer [frothing-berserker]]
            [firestone.events.queue :refer [get-events
                                            enqueue-event
                                            qpeek
                                            dequeue
                                            enqueue]]
            [firestone.events.handlers :refer [event-map]]
            [firestone.util :refer [flip-partial]]))


(deftest moroes
         (is (-> (create-game [{:minions ["Moroes"]}])
                 (enqueue-event :end-turn)
                 (step-events)))
         (is (-> (create-game)
                 (enqueue-event :summon-minion "p1" "Steward")
                 (step-events))))

(deftest damage-frothing-berserker
         (let [event {:event-type :damage-minion
                      :minion-id  1
                      :damage     2}]
           (let [state (-> (create-game [{:minions [(create-minion "Frothing Berserker" :id 1)
                                                    (create-minion "Frothing Berserker" :id 2)]}])
                           (enqueue-event event))]
             (is= (-> (run-triggers state event)
                      (get-minion 1)
                      (:attack))
                  (-> (get-minion state 1)
                      (get-definition)
                      (:base-attack)
                      (inc)))
             (is= (-> (run-triggers state event)
                      (get-minion 2)
                      (:attack))
                  (-> (get-minion state 1)
                      (get-definition)
                      (:base-attack)
                      (inc)))
             (is= (-> (run-triggers state event)
                      (get-minion 1)
                      (:attack))
                  (-> (get-minion state 2)
                      (get-definition)
                      (:base-attack)
                      (inc))))
           (let [state (-> (create-game [{:minions [(create-minion "Frothing Berserker" :id 1)]}
                                         {:minions [(create-minion "War Golem" :id 2)]}])
                           (enqueue-event event))
                 minions (get-minions state)]
             (is= (-> (run-triggers state event)
                      (get-minion 1)
                      (:attack))
                  (-> (first minions)
                      (get-definition)
                      (:base-attack)
                      (inc)))
             (is= (-> state
                      (enqueue-event :attack-minion 1 2)
                      (step-events)
                      (get-minion 2)
                      (:damage-taken))
                  3))))

(deftest loot-hoarder
         (let [event {:event-type :destroy-minion
                      :player-id  "p1"
                      :minion-id  1}
               state (create-game
                       [{:minions
                         [(create-minion "Loot Hoarder" :id 1)]}])]
           (is= (-> (run-triggers state event)
                    (get-events))
                [{:event-type :draw-card
                  :player-id  "p1"}]))
         (let [event {:event-type :destroy-minion
                      :player-id  "p1"
                      :minion-id  1}
               state (-> (create-game
                           [{:minions
                             [(create-minion "Loot Hoarder" :id 1)]}])
                         (assoc :events [event]))]
           (is= (-> (step-event state)
                    (get-events))
                [{:event-type :draw-card
                  :player-id  "p1"}])))

(deftest test-step-event
  (is= (-> (create-game
             [{:minions ["Acolyte of Pain"]}])
           (enqueue-event {:event-type :damage-minion
                           :minion-id  "m1"
                           :damage     2})
           (step-event)
           (get-events))
       [{:event-type :draw-card
         :player-id  "p1"}])
  ;; Trigger Tests
  (let [event {:event-type  :attack-minion
               :attacker-id 1
               :target-id   2}
        state (-> (create-game
                    [{:minions [(create-minion "Imp" :id 1)]}
                     {:minions [(create-minion "War Golem" :id 2)]}])
                  (play-spell-card "p2" {:name        "Snake Trap"
                                         :entity-type :card
                                         :id          "c3"})
                  (enqueue-event event))]
    (is= (->> state
              (step-event)
              (:events)
              (qpeek))
         {:event-type  :summon-minion
          :player-id   "p2"
          :minion-name "Snake"})
    (is= (->> state
              (step-event)
              (get-events)
              (map :event-type))
         (list :summon-minion
               :summon-minion
               :summon-minion
               :damage-minion
               :damage-minion))
    (is= (-> (create-game [{:minions [(create-minion "Frothing Berserker" :id 1)]}
                           {:minions [(create-minion "War Golem" :id 2)]}])

             (enqueue-event :attack-minion 1 2)
             (enqueue-event :damage-minion 1 2)
             (step-events)
             (step-event))
         nil)))

(deftest test-step-events
  (let [event {:event-type :draw-card
               :player-id  "p1"}
        state (-> (init-game ["Jaina Proudmoore", "Anduin Wrynn"])
                  (enqueue-event event))]
    (is= (-> state
             (step-events)
             (get-in [:players "p1" :hand])
             (count))
         (-> state
             (get-in [:players "p1" :hand])
             (count)
             (inc))))

  (let [state (init-game ["Jaina Proudmoore", "Anduin Wrynn"])]
    (is= (-> state
             (step-events))
         state))
  (let [events (repeat 3 {:event-type :draw-card
                          :player-id  "p1"})
        state (-> (init-game ["Jaina Proudmoore", "Anduin Wrynn"])
                  (assoc :events events))]
    (is= (-> state
             (step-events)
             (get-in [:players "p1" :hand])
             (count))
         (-> state
             (get-in [:players "p1" :hand])
             (count)
             (+ 3)))
    (is (-> state
            (step-events)
            (get-events)
            (empty?))))
  (let [state (-> (create-game
                    [{:minions [(create-minion "Imp" :id 1)
                                (create-minion "Lorewalker Cho" :id 3)]}
                     {:minions [(create-minion "War Golem" :id 2)]}]))]
    (is= (-> state
             (enqueue-event :play-card "p1" (get-definition "Snake Trap"))
             (enqueue-event :attack-minion 1 2)
             (step-events)
             (get-in [:players "p2" :hand])
             (->> (map :name)))
         ["Snake Trap"])
    (is= (-> state
             (enqueue-event :play-card "p2" (get-definition "Fireball"))
             (enqueue-event :attack-minion 1 2)
             (step-events)
             (get-in [:players "p1" :hand])
             (->> (map :name)))
         ["Fireball"]))
  (let [state (-> (create-game
                    [{:minions [(create-minion "Imp" :id 1)
                                (create-minion "Archmage Antonidas" :id 4)]}
                     {:minions [(create-minion "War Golem" :id 2)]}]))]
    (is= (-> state
             (enqueue-event :play-card "p1" (get-definition "Snake Trap"))
             (enqueue-event :attack-minion 1 2)
             (step-events)
             (get-in [:players "p1" :hand])
             (->> (map :name)))
         ["Fireball"])
    (is= (-> state
             (enqueue-event :play-card "p1" (get-definition "Fireball"))
             (enqueue-event :attack-minion 1 2)
             (step-events)
             (get-in [:players "p1" :hand])
             (->> (map :name)))
         ["Fireball"])))