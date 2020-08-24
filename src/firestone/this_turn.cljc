(ns firestone.this-turn
  (:require [ysera.test :refer [deftest is is=]]
            [firestone.construct :refer [create-game
                                         create-minion]]
            [firestone.actions :refer [play-spell-card]]
            [firestone.events.queue :refer [enqueue-event]]
            [firestone.events.stepper :refer [step-event
                                              step-events]]))


(deftest event-log
         (is= (-> (create-game
                    [{:minions [(create-minion "Imp" :id 1)]}
                     {:minions [(create-minion "War Golem" :id 2)]}])
                  (play-spell-card "p2" {:name        "Snake Trap"
                                         :entity-type :card
                                         :id          "c3"})
                  (enqueue-event :attack-minion 1 2)
                  (step-events)
                  (enqueue-event :end-turn)
                  (step-events)
                  (:turn))
              (-> (create-game
                    [{:minions [(create-minion "Imp" :id 1)]}
                     {:minions [(create-minion "War Golem" :id 2)]}])
                  (play-spell-card "p2" {:name        "Snake Trap"
                                         :entity-type :card
                                         :id          "c3"})
                  (enqueue-event :attack-minion 1 2)
                  (step-events)
                  (dissoc :turn)
                  (enqueue-event :draw-card "p2")
                  (enqueue-event :end-turn)
                  (step-events)
                  (:turn))))
