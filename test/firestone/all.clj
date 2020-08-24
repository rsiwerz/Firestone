(ns firestone.all
  (:require [ysera.test :refer [deftest is is= is-not]]
            [clojure.test :refer [successful? run-tests]]
            ;; definition
            [firestone.definitions-loader]
            [firestone.definition.card]
            [firestone.definition.hero]
            [firestone.definitions]
            [firestone.definition.minion-modifiers]
            ;; events
            [firestone.events]
            [firestone.events.battlecries]
            [firestone.events.deathrattles]
            [firestone.events.handlers]
            [firestone.events.queue]
            [firestone.events.spells]
            [firestone.events.stepper]
            ;; etc
            [firestone.events.triggers]
            [firestone.actions]
            [firestone.api-functions]
            [firestone.construct]
            [firestone.core]
            [firestone.edn-api]
            [firestone.instrumentation]
            [firestone.mapper]
            [firestone.server]
            [firestone.spec]
            [firestone.this-turn]
            [firestone.util]
            ;; test
            [firestone.test-actions]
            [firestone.test-battlecries]
            [firestone.test-deathrattles]
            [firestone.test-heropower]))


(deftest test-all
         "Bootstrapping with the required namespaces, finds all the firestone.* namespaces (except this one),
         requires them, and runs all their tests."
         (let [namespaces (->> (all-ns)
                               (map str)
                               (filter (fn [x] (re-matches #"firestone\..*" x)))
                               (remove (fn [x] (= "firestone.all" x)))
                               (map symbol))]
           (is (successful? (time (apply run-tests namespaces))))))

(deftest definitions
         "Test that the name of every definition is also the key."
         (is= (->> (firestone.definitions/get-definitions)
                   (map :name)
                   (map firestone.definitions/get-definition))
              (firestone.definitions/get-definitions))
         ;; Test that no card contains an :attack key.
         (is (every? #(not (contains? % :attack)) (firestone.definitions/get-definitions))))
