(ns firestone.test-deathrattles
  (:require [ysera.test :refer [deftest is is=]]
            [clojure.test :refer [successful? run-tests]]
            [firestone.definitions :refer [get-definition]]
            [firestone.definitions-loader]
            [firestone.definition.minion-modifiers :refer [increase-attack-this-turn]]
            [firestone.construct :refer [get-minion
                                         get-minions
                                         create-minion
                                         create-game
                                         add-minion-to-board
                                         create-hero]]
            [firestone.actions :refer :all]
            [firestone.events.queue :refer [enqueue-event
                                            get-events]]
            [firestone.events.stepper :refer [step-events
                                              step-event]]
            [firestone.events.deathrattles :refer [deranged-doctor]]))

(deftest deranged-doctor-test
         (let [event {:event-type :destroy-minion
                      :player-id "p1"
                      :minion-id "m1"}
               state (-> (create-game [{:hero (create-hero "Rexxar" :damage-taken 9)}])
                         (add-minion-to-board {:player-id "p1"
                                               :minion (create-minion "Deranged Doctor" :id "m1")
                                               :position 0}))]
           (is= (as-> state $
                      (deranged-doctor event (get-minion state "m1") $)
                      (step-event $)
                      (get-in $ [:players "p1" :hero])
                      (:damage-taken $))
                1)))


