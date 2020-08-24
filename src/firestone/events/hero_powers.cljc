(ns firestone.events.hero-powers
  (:require [firestone.construct :refer [create-game
                                         create-minion
                                         create-hero
                                         get-heroes
                                         get-minion
                                         get-opponent-id
                                         get-minions]]
            [firestone.core :refer [get-character]]
            [firestone.events.queue :refer [enqueue-event
                                            get-events]]
            [ysera.test :refer [is=]]))


(defn valid-fireblast-targets
  [state]
  (let [heroes (get-heroes state)
        minions (get-minions state)]
    (map :id (concat heroes minions))))

(defn fireblast
  "Deal 1 damage."
  [state player-id character-id]
  (if (-> (get-character state character-id)
          :entity-type
          (= :hero))

    (-> (enqueue-event state :damage-hero character-id 1))
    (-> (enqueue-event state :damage-minion character-id 1))))

(defn valid-lesser-heal-targets
  [state]
  (let [heroes (get-heroes state)
        minions (get-minions state)]
    (map :id (concat heroes minions))))

(defn lesser-heal
  "Restore 2 health."
  {:test (fn []
           (let [state (create-game [{:hero (create-hero "Anduin Wrynn" :owner-id "p1")}
                                     {:minions [(create-minion "War Golem" :damage-taken 3)]}])]
             (is= (-> state
                      (lesser-heal "p1" "m1")
                      (get-events))
                  [{:event-type    :restore-health
                    :character-id  "m1"
                    :health        2}])))}
  [state player-id character-id]
  (enqueue-event state :restore-health character-id 2))


(defn steady-shot
  "Deal 2 damage to the enemy hero."
  {:test (fn []
           (let [state (create-game [{:hero "Rexxar"}])]
             (is= (-> state
                      (steady-shot "p1" nil)
                      (get-events))
                  [{:event-type :damage-hero
                    :player-id "p2"
                    :damage 2}])
             (is= (-> state
                      (steady-shot "p2" nil)
                      (get-events))
                  [{:event-type :damage-hero
                    :player-id "p1"
                    :damage 2}])))}
  [state player-id _]
  (let [enemy-id (get-opponent-id state player-id)]
    (enqueue-event state :damage-hero enemy-id 2)))

(defn reinforce
  {:test (fn []
           (let [state (create-game [{:hero (create-hero "Uther Lightbringer")}])]
             (is= (-> state
                      (reinforce "p1" "something")
                      (get-events))
                  [{:event-type :summon-minion
                    :player-id "p1"
                    :minion-name "Silver Hand Recruit"}])
             (is= (-> state
                      (reinforce "p2" "something")
                      (get-events))
                  [{:event-type :summon-minion
                    :player-id "p2"
                    :minion-name "Silver Hand Recruit"}])))}
  [state player-id hero-id]
  (enqueue-event state :summon-minion player-id "Silver Hand Recruit"))