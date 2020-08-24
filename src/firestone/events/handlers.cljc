(ns firestone.events.handlers
  (:require [ysera.test :refer [is is-not is= error?]]
            [firestone.actions :refer [draw-card
                                       damage-minion
                                       damage-hero
                                       destroy-minion
                                       summon-minion
                                       attack-minion
                                       attack-hero
                                       play-minion-card
                                       play-spell-card
                                       end-turn
                                       use-hero-power
                                       start-turn]]
            [firestone.core :refer [get-character
                                    heal-character]]
            [firestone.construct :refer [update-hero
                                         add-minion-to-graveyard
                                         get-minion]]))
(def event-map
  {:draw-card
   (fn [state event]
     (draw-card state 1 (:player-id event)))
   :damage-minion
   (fn [state event]
     (assert (some? (:damage event))
             (str ":damage is nil in" event))
     (damage-minion state (:minion-id event) (:damage event)))
   :damage-hero
   (fn [state {player-id :player-id damage :damage}]
     (damage-hero state player-id damage))
   :summon-minion
   (fn [state {player :player-id name :minion-name minion :minion pos :position}]
     (assert (or name minion))
     (if pos
       (summon-minion state player (if minion
                                     minion
                                     name)
                      pos)
       (summon-minion state player (if minion
                                     minion
                                     name))))
   :destroy-minion
   (fn [state {player :player-id minion-id :minion-id}]
     (let [minion (get-minion state minion-id)]
       (-> (add-minion-to-graveyard state minion-id)
           (destroy-minion player minion-id))))
   :attack-minion
   (fn [state {attacker :attacker-id target :target-id}]
     (attack-minion state target attacker))
   :attack-hero
   (fn [state {attacker :attacker-id target :target-id}]
     (attack-hero state target attacker))
   :use-hero-power
   (fn [state {player-id :player-id target-id :target-id}]
     (use-hero-power state player-id target-id))
   :play-card
   (fn [state {player-id :player-id card :card type :card-type position :position target-id :target-id}]
     (assert (some? type))
     (if (= type :minion)
       (play-minion-card state player-id card position target-id)
       (play-spell-card state player-id card)))
   :start-of-turn
   (fn [state {player-id :next-player}]
     (start-turn state player-id))
   :end-turn
   (fn [state {player-id :current-player}]
     (end-turn state player-id))
   :restore-health
   (fn [state {character-id :character-id health :health}]
     (heal-character state character-id health))})
