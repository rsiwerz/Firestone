(ns firestone.test-heropower
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
            [firestone.events.hero-powers :refer [lesser-heal]]))

(deftest lesser-heal-test
         (let [state (create-game [{:hero (create-hero "Anduin Wrynn" :owner-id "p1")}
                                   {:minions [(create-minion "War Golem" :damage-taken 3)]}])]
           (is= (-> state
                    (lesser-heal "p1" "m1")
                    (step-event)
                    (get-minion "m1")
                    (:damage-taken))
                1)))






