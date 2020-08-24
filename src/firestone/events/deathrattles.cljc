(ns firestone.events.deathrattles
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.random :refer [random-nth]]
            [firestone.construct :refer [add-minion-to-board
                                         create-card
                                         create-game
                                         create-hero
                                         get-players
                                         get-minion
                                         get-dead-minion
                                         get-dead-minions
                                         get-minions
                                         create-minion
                                         get-seed
                                         set-seed
                                         remove-minion]]
            [firestone.core :refer [heal-character]]
            [firestone.util :refer [trace]]
            [firestone.definitions :refer [get-definitions
                                           get-definition]]
            [firestone.events.queue :refer [get-events
                                            enqueue-event]]))

(defn killed?
  [{type :event-type target :minion-id} minion-id]
  (and (= type :destroy-minion)
       (= target minion-id)))

(defn loot-hoarder
  "Deathrattle: Draw a card."
  {:test (fn []
           (let [event {:event-type :destroy-minion
                        :player-id "p1"
                        :minion-id  "m1"}
                 state (create-game
                         [{:minions ["Loot Hoarder"]}])
                 minion (get-minion state "m1")]
             (is= (-> (loot-hoarder event minion state)
                      (get-events))
                  [{:event-type :draw-card
                    :player-id  "p1"}])))}
  [event self state]
  (if (killed? event (:id self))
    (enqueue-event state {:event-type :draw-card
                          :player-id  (:owner-id self)})
    state))

(defn deranged-doctor
  "Deathrattle: Restore 8 Health to your hero."
  {:test (fn []
           (let [event {:event-type :destroy-minion
                        :player-id "p1"
                        :minion-id "m1"}
                 state (-> (create-game [{:hero (create-hero "Rexxar" :damage-taken 9)}])
                           (add-minion-to-board {:player-id "p1"
                                                 :minion (create-minion "Deranged Doctor" :id "m1")
                                                 :position 0}))]
             (is= (->> state
                       (deranged-doctor event (get-minion state "m1"))
                       (get-events))
                  [{:event-type    :restore-health
                    :character-id  "h1"
                    :health        8}])))}
  [event self state]
  (if (killed? event (:id self))
    (let [owner (:owner-id self)
          hero-id (get-in state [:players owner :hero :id])]
      (enqueue-event state :restore-health hero-id 8))
    state))

(defn sylvanas-windrunner
  "Deathrattle: Take control of a random enemy minion."
  {:test (fn []
           (let [event {:event-type :destroy-minion
                        :player-id "p2"
                        :minion-id "m3"}
                 state (create-game [{:minions ["Imp" "War Golem"]}
                                     {:minions ["Sylvanas Windrunner"]}])
                 minion (get-minion state "m3")]
             (is= (as-> (set-seed state 2) $
                    (sylvanas-windrunner event minion $)
                    (get-minions $ "p2")
                    (map :name $))
                  ["Sylvanas Windrunner" "Imp"])
             (is= (as-> (set-seed state 2) $
                    (sylvanas-windrunner event minion $)
                    (get-minions $ "p1")
                    (map :name $))
                  ["War Golem"])))}
  [event self state]
  (if (killed? event (:id self))
    (let [owner (:owner-id self)
          seed (get-seed state)
          target-player (->> (get-players state)
                             (filter (fn [player]
                                       (not= owner (:id player))))
                             (first)
                             (:id))
          target-minions (get-minions state target-player)
          [new-seed random-minion] (random-nth seed target-minions)]
      (if random-minion
        (-> (remove-minion state (:id random-minion))
            (add-minion-to-board {:player-id owner
                                  :minion random-minion
                                  :position (:position self)})

            (set-seed new-seed))
        state))
    state))

(defn sneeds-old-shredder
  "Deathrattle: Summon a random Legendary minion."
  {:test (fn []
           (let [event {:event-type :destroy-minion
                        :player-id "p1"
                        :minion-id "m1"}
                 state (create-game [{:minions ["Sneed's Old Shredder"]}])
                 minion (get-minion state "m1")]
             (is= (as-> (set-seed state 4) $
                        (sneeds-old-shredder event minion $)
                        (get-events $))
                  [{:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Sneed's Old Shredder"}])
             (is= (as-> (set-seed state 5) $
                    (sneeds-old-shredder event minion $)
                    (get-events $))
                  [{:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Trade Prince Gallywix"}])))}
  [event self state]
  (if (killed? event (:id self))
    (let [seed (get-seed state)
          owner-id (:owner-id self)
          minions (filter (fn [minion]
                            (= (:rarity minion) :legendary))
                          (get-definitions))
          [new-seed minion] (random-nth seed minions)]
      (-> (set-seed state new-seed)
          (enqueue-event :summon-minion owner-id (:name minion))))
    state))


(defn hadronox
  "Deathrattle: Summon your Taunt minion that died this game."
  {:test (fn []
           (let [event {:event-type :destroy-minion
                        :player-id "p1"
                        :minion-id "m1"}
                 state (create-game [{:minions ["Hadronox"]}])
                 minion (get-minion state "m1")]
             (is= (as-> state $
                    (assoc $ :graveyard [(create-minion "Shieldbearer" :id "s1")
                                         (create-minion "Shieldbearer" :id "s2")
                                         (create-minion "Imp" :id "i1")])
                    (hadronox event minion $)
                    (get-events $))
                  [{:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Shieldbearer"}
                   {:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Shieldbearer"}])
             (is= (as-> state $
                    (assoc $ :graveyard [(create-minion "Imp" :id "i1")])
                    (hadronox event minion $)
                    (get-events $))
                  nil)))}
  [event self state]
  (if (killed? event (:id self))
    (let [event-log (get-in state [:turn :event-log])
          dead-minions (get-dead-minions state)
          taunt-minions (-> (filter (fn [m]
                                      (if (:description (get-definition m))
                                        (= (:description (get-definition m)) "Taunt")))
                                    dead-minions))
          owner-id (:owner-id self)]
      (if (some? taunt-minions)
        (reduce (fn [state taunt-minion]
                  (enqueue-event state :summon-minion owner-id (:name taunt-minion)))
                state taunt-minions)))
    state))

(defn nzoth-the-corruptor
  "Deathrattle: Summon your Deathrattle minions that died this game."
  {:test (fn []
           (let [event {:event-type :destroy-minion
                        :player-id "p1"
                        :minion-id "m1"}
                 state (-> (create-game [{:minions ["N'Zoth, the Corruptor"]}]))
                 minion (get-minion state "m1")]
             (is= (as-> state $
                    (assoc $ :graveyard [(create-minion "Sylvanas Windrunner" :id "s1")
                                         (create-minion "Sneed's Old Shredder" :id "s2")
                                         (create-minion "Imp")])
                    (nzoth-the-corruptor event minion $)
                    (get-events $))
                  [{:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Sylvanas Windrunner"}
                   {:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Sneed's Old Shredder"}])
             (is= (as-> state $
                    (assoc $ :graveyard [(create-minion "Imp")])
                    (nzoth-the-corruptor event minion $)
                    (get-events $))
                  nil)
             ))}
  [event self state]
  (if (killed? event (:id self))
    (let [dead-minions (get-dead-minions state)
          deathrattle-minions (filter (fn [m]
                                        (= (:deathrattle (get-definition m)) true))
                                      dead-minions)
          owner-id (:owner-id self)]
      (if (some? deathrattle-minions)
        (reduce (fn [state {name :name}]
                  (enqueue-event state
                                 :summon-minion
                                 owner-id
                                 name))
                state
                deathrattle-minions)
        state))
    state))
