(ns firestone.test-battlecries
  (:require [ysera.test :refer [deftest is is=]]
            [clojure.test :refer [successful? run-tests]]
            [firestone.definitions :refer [get-definition]]
            [firestone.definitions-loader]
            [firestone.definition.minion-modifiers :refer [increase-attack-this-turn]]
            [firestone.construct :refer [get-minion
                                         get-minions
                                         create-minion
                                         create-game]]
            [firestone.actions :refer :all]
            [firestone.events.queue :refer [enqueue-event
                                            get-events]]
            [firestone.events.stepper :refer [step-events
                                              step-event]]
            [firestone.events.battlecries :refer [shrinkmeister
                                                  abusive-sergeant]]
            [firestone.util :refer [flip-partial]]))


(deftest reset-turnbuffs
         (let [state (create-game [{:minions [(create-minion "War Golem" :id "w1")]}])]
           (is= (-> (shrinkmeister state "p2" (get-minion state "w1"))
                    (enqueue-event :end-turn)
                    (step-events)
                    (get-minion "w1")
                    (:attack))
                (-> (get-definition "War Golem")
                    (:base-attack)))

           (is= (-> (shrinkmeister state "p2" (get-minion state "w1"))
                    (enqueue-event :end-turn)
                    (step-events)
                    (get-minion "w1"))
                (-> (get-minion state "w1"))))

         (let [state (create-game [{:minions [(create-minion "Imp" :id "i1")]}])]
           (is= (-> (abusive-sergeant state "p2" (get-minion state "i1"))
                    (enqueue-event :end-turn)
                    (step-events)
                    (get-minion "i1")
                    (:attack))
                (-> (get-definition "Imp")
                    (:base-attack)))

           (is= (-> (abusive-sergeant state "p2" (get-minion state "i1"))
                    (enqueue-event :end-turn)
                    (step-events)
                    (get-minion "w1"))
                (-> (get-minion state "w1"))))
         (let [state (create-game [{:minions ["Imp"]}
                                   {:minions ["War Golem"]}])]
           (is= (-> (abusive-sergeant state "p1" (get-minion state "m1"))
                    (enqueue-event :attack-minion "m1" "m2")
                    (step-event)
                    (get-events))
                [{:event-type :damage-minion, :minion-id "m2", :damage 3}
                 {:event-type :damage-minion, :minion-id "m1", :damage 7}])
           (is= (-> (abusive-sergeant state "p1" (get-minion state "m1"))
                    (enqueue-event :attack-minion "m1" "m2")
                    (->>
                      (step-event)
                      (step-event)
                      (step-event)
                      (get-events)))
                [{:event-type :destroy-minion, :player-id "p1", :minion-id "m1"}])))

(deftest buff-this-turn
         (let [state (as-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]) st
                           (increase-attack-this-turn st 5 (get-minion st "i")))]
           (is= (-> (create-game [{:minions [(create-minion "Acolyte of Pain" :id "i")]}])
                    (get-minion "i"))
                (-> (create-game [{:minions [(create-minion "Acolyte of Pain" :id "i")]}])
                    (get-in [:players "p1" :minions])
                    (first)
                    (merge (get-definition "Acolyte of Pain"))))))