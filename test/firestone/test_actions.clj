(ns firestone.test-actions
  (:require [ysera.test :refer [deftest is is= is-not]]
            [clojure.test :refer [successful? run-tests]]
            [firestone.definitions]
            [firestone.definitions-loader]
            [firestone.construct :refer [get-minion
                                         get-player
                                         create-game
                                         create-card]]
            [firestone.core :refer :all]
            [firestone.actions :refer :all]
            [firestone.events.stepper :refer [step-events]]
            [firestone.events.queue :refer [enqueue-event]]
            [firestone.events.battlecries :refer [eater-of-secrets]]
            [firestone.util :refer [trace]]))

(deftest test-end-turn
         (is= (let [state (init-game ["Jaina Proudmooore" "Rexxar"])]
                (-> (end-turn state "p1")
                    (step-events)
                    (get-in [:players "p1" :deck])
                    (count)))
              27)
         (is= (let [state (init-game ["Jaina Proudmooore" "Rexxar"])]
                (-> (end-turn state "p1")
                    (step-events)
                    (get-in [:players "p1" :hand])
                    (count)))
              3)
         (is= (let [state (init-game ["Jaina Proudmooore" "Rexxar"])]
                (-> (end-turn state "p1")
                    (step-events)
                    (get-in [:players "p2" :deck])
                    (count)))
              25)
         (is= (let [state (init-game ["Jaina Proudmooore" "Rexxar"])]
                (-> (end-turn state "p1")
                    (step-events)
                    (get-in [:players "p2" :hand])
                    (count)))
              5))

(deftest test-play-minion-card
         (is= (let [card (create-card "War Golem" :id "c1")
                    state (create-game [{:hand [card]}])]
                (-> (play-minion-card state "p1" card 0)
                    (get-hand "p1")))
              (list))
         (is= (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
                    card (first (get-in state [:players "p1" :hand]))]
                (-> (play-minion-card state "p1" card 0)
                    (step-events)
                    (get :minion-ids-summoned-this-turn)))
              ["m4"])
         (is= (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
                    card (first (get-in state [:players "p1" :hand]))]
                (-> (play-minion-card state "p1" card 2)
                    (step-events)
                    (get :minion-ids-summoned-this-turn)))
              ["m4"])
         (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
               attacker (first (get-in state [:players "p1" :hand]))
               target (get-in state [:players "p2"])]
           ;; Bad test:
           (comment is (as-> (enqueue-event state :play-card attacker "p1" 0) $
                     (enqueue-event $ :end-turn)
                     (enqueue-event $ :end-turn)
                     (step-events $)
                     (valid-attack? $ "p1" (:id (first (get-in $ [:players "p1" :minions]))) (:id target))))

           (comment is-not (as-> (play-minion-card state "p1" attacker 0) $
                         (valid-attack? $ (:player-id-in-turn $) (:id (first (get-in $ [:players "p1" :minions]))) (:id target))))))


(deftest test-play-spell-card
         (is= (let [card (create-card "Snake Trap" :id "c1")
                    state (create-game [{:hand [card]}])]
                (-> (play-spell-card state "p1" card)
                    (get-hand "p1")))
              (list))
         (is= (let [card (create-card "Snake Trap" :id "c1")
                    state (create-game [{:hand [card]}])]
                (-> (play-spell-card state "p1" card)
                    (step-events)
                    (play-minion-card "p2" (create-card "Eater of Secrets") 1)
                    (eater-of-secrets "p2" {})
                    (step-events)
                    (get-player "p1")
                    (:secrets)))
              nil)
         (let [card (create-card "Snake Trap" :id "c1")
               state (create-game [{:hand [card]}])]
           (is= (-> (play-spell-card state "p1" card)
                    (step-events)
                    (play-minion-card "p2" (create-card "Eater of Secrets") 1)
                    (eater-of-secrets "p2" {})
                    (step-events)
                    (get-minion "m2")
                    (select-keys [:name :attack :health]))
                {:name "Eater of Secrets"
                 :attack 3
                 :health 5})))
