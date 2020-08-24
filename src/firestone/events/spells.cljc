(ns firestone.events.spells
  (:require [ysera.test :refer [is=]]
            [firestone.construct :refer [add-minion-to-board
                                         create-card
                                         create-game
                                         create-minion
                                         create-secret
                                         get-secrets
                                         get-minion
                                         get-minions
                                         get-players
                                         get-heroes
                                         get-opponent-id]]
            [firestone.core :refer [get-character
                                    get-character-states
                                    enough-mana?
                                    add-mana]]
            [firestone.definitions :refer [get-definition]]
            [firestone.definition.minion-modifiers :refer [increase-attack
                                                           increase-health
                                                           set-character-state
                                                           buff-entity-this-turn
                                                           remove-character-state]]
            [firestone.events.queue :refer [get-events
                                            qpeek
                                            enqueue-event]]))

(defn fireball-valid-targets
  "Find all the valid taraget for fireball"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]}
                                     {:minions ["Imp" "Abusive Sergeant"]
                                      :hand ["Fireball"]}])]
             (is= (-> state
                      (fireball-valid-targets))
                  ["h1" "h2" "m1" "m2" "m3" "m4" "m5"])))}
  [state]
  (let [minions (get-minions state)
        heroes (get-heroes state)
        targets (concat heroes minions)]
    (map :id targets)))

(defn fireball
  "Deal 6 damage."
  {:test (fn []
           (let [minions ["Imp" "War Golem"]
                 hand ["Fireball"]
                 state (create-game [{:minions minions
                                      :hand    hand}])]
             (is= (-> state
                      (fireball "p1" "m2")
                      (get-events))
                  (-> (enqueue-event {} :damage-minion "m2" 6)
                      (get-events)))
             (is= (-> state
                      (add-minion-to-board {:player-id "p1"
                                            :minion (create-minion "Malygos")
                                            :position 3})
                      (fireball "p1" "m2")
                      (get-events))
                  (-> (enqueue-event {} :damage-minion "m2" 11)
                      (get-events)))))}
  [state owner target-id]
  (let [minions (get-minions state owner)
        spell-damage (reduce
                      (fn [acc minion]
                        (let [spell (:spell-damage (get-definition minion))]
                          (if spell
                            (+ spell acc)
                            acc)))
                      0 minions)]
    (if (= (:type (get-character state target-id)) :minion)
      (enqueue-event state :damage-minion target-id (+ 6 spell-damage))
      (enqueue-event state :damage-hero target-id (+ 6 spell-damage)))))

(defn bananas-valid-targets
  "Find all the valid targets for bananas"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]}
                                     {:minions ["Imp" "Abusive Sergeant"]
                                      :hand ["Bananas"]}])]
             (is= (-> state
                      (bananas-valid-targets))
                  ["m1" "m2" "m3" "m4" "m5"])))}
  [state]
  (->> (get-minions state)
       (map :id)))

(defn bananas
  "Give a minion +1/+1"
  {:test (fn []
           (let [minions ["Imp" "War Golem"]
                 hand ["Bananas"]
                 state (create-game [{:minions minions
                                      :hand    hand}])]
             (is= (-> state
                      (bananas "p1" "m1")
                      (get-minion "m1")
                      (:attack))
                  (-> (get-definition "Imp")
                      (:base-attack)
                      (+ 1)))
             (is= (-> state
                      (bananas "p1" "m1")
                      (get-minion "m1")
                      (:health))
                  (-> (get-definition "Imp")
                      (:health)
                      (+ 1)))))}
  [state owner minion-id]
  (let [minion (get-minion state minion-id)
        minions (get-minions state owner)
        spell-damage (reduce
                      (fn [acc minion]
                        (let [spell (:spell-damage (get-definition minion))]
                          (if spell
                            (+ spell acc)
                            acc)))
                      0 minions)]
    (-> state
        (increase-attack (+ 1 spell-damage) minion)
        (increase-health (+ 1 spell-damage) minion))))

(defn valid-mind-control-targets
  "Get the valid enemy targets for the card: mind control"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]}
                                     {:minions ["Abusive Sergeant" "Big Game Hunter"]
                                      :hand ["Mind Control"]}]
                                    :player-id-in-turn "p2")]
             (is= (-> state
                      (valid-mind-control-targets))
                  ["m1" "m2" "m3"])))}
  [state]
  (let [owner (:player-id-in-turn state)
        target-player (->> state
                           (get-players)
                           (filter (fn [player] (not= (:id player) owner)))
                           (map :id)
                           (first))]
    (map :id (get-minions state target-player))))

(defn mind-control
  "Take control of an enemy minion."
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]}
                                     {:hand ["Mind Control"]}])]
             (is= (-> state
                      (mind-control "p2" "m2")
                      (get-events))
                  [{:event-type :destroy-minion
                    :player-id  "p1"
                    :minion-id  "m2"}
                   {:event-type :summon-minion
                    :player-id  "p2"
                    :minion       (get-minion state "m2")
                    :position   0}])))}
  [state owner minion-id]
  (let [minion (get-minion state minion-id)
        target-player (->> state
                           (get-players)
                           (filter (fn [player] (not= (:id player) owner)))
                           (map :id)
                           (first))]
    (-> (enqueue-event state :destroy-minion target-player minion-id)
        (enqueue-event :summon-minion owner minion 0))))

(defn valid-rampage-targets
  "Get the damaged minions that rampage can buff"
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "Imp")
                                                (create-minion "War Golem" :damage-taken 3)
                                                (create-minion "Arcane Golem" :damage-taken 1)]}
                                     {:hand ["Rampage"]}])]
             (is= (-> state
                      (valid-rampage-targets))
                  ["m2" "m3"])))}
  [state]
  (let [damaged-minions (->> (get-minions state)
                             (filter (fn [minion] (> (:damage-taken minion) 0))))]
    (map :id damaged-minions)))


(defn frostbolt-valid-targets
  "Find all valid targets for frostbolt"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]}
                                     {:minions ["Imp" "Abusive Sergeant"]
                                      :hand ["Frostbolt"]}])]
             (is= (frostbolt-valid-targets state)
                  ["h1" "h2" "m1" "m2" "m3" "m4" "m5"])))}
  [state]
  (let [minions (get-minions state)
        heroes (get-heroes state)
        targets (concat heroes minions)]
    (map :id targets)))

(defn frostbolt
  "Spell: Deal 3 damage and then Freeze the target for 1 turn"
  {:test (fn []
           (let [minions ["Imp" "War Golem"]
                 hand ["Frostbolt"]
                 state (create-game [{:minions minions
                                      :hand    hand}])]
             (is= (-> state
                      (frostbolt "p1" "m2")
                      (get-events))
                  (-> (enqueue-event {} :damage-minion "m2" 3)
                      (get-events)))
             (is= (-> state
                      (add-minion-to-board {:player-id "p1"
                                            :minion (create-minion "Malygos")
                                            :position 3})
                      (frostbolt "p1" "m2")
                      (get-events))
                  (-> (enqueue-event {} :damage-minion "m2" 8)
                      (get-events)))
             (is= (-> state
                      (frostbolt "p1" "m2")
                      (get-character-states "m2"))
                  ["FROZEN"])))}
  [state owner target-id]
  (let [minions (get-minions state owner)
        spell-damage (reduce
                       (fn [acc minion]
                         (let [spell (:spell-damage (get-definition minion))]
                           (if spell
                             (+ spell acc)
                             acc)))
                       0 minions)]
    (-> (if (= (:type (get-character state target-id)) :minion)
          (enqueue-event state :damage-minion target-id (+ 3 spell-damage))
          (enqueue-event state :damage-hero target-id (+ 3 spell-damage)))
        (set-character-state target-id "FROZEN"))))

(defn flare
  {:test (fn []
           (let [state (create-game [{:secrets [(create-secret "Snake Trap" :id "s1")]}])]
             (is= (-> state
                      (flare "p2" "")
                      (get-secrets "p1"))
                  []))
           (let [state (create-game [{:minions [(create-minion "Blood Imp" :id "bi")]}])]
             (is= (-> state
                      (flare "p2" "")
                      (get-character-states "bi"))
                  []))
           (let [state (create-game)]
             (is= (-> state
                      (flare "p1" "")
                      (get-events))
                  [{:event-type :draw-card
                    :player-id  "p1"}])))}
  [state owner-id target-id]
  (let [all-minions (get-minions state)]
    (as-> state $
          (update-in $ [:players (get-opponent-id state owner-id)] dissoc :secrets)
          (enqueue-event $ :draw-card (:player-id-in-turn state))
          (reduce (fn [s m]
                    (remove-character-state s (:id m) "STEALTH"))
                  $
                  all-minions))))

(defn rampage
  "Give a damaged minion +3/+3"
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "War Golem" :damage-taken 3)]
                                      :hand ["Rampage"]}])]
             (is= (-> state
                      (rampage "p1" "m1")
                      (get-minion "m1")
                      (:attack))
                  (-> (get-definition "War Golem")
                      (:base-attack)
                      (+ 3)))
             (is= (-> state
                      (rampage "p1" "m1")
                      (get-minion "m1")
                      (:health))
                  (-> (get-definition "War Golem")
                      (:health)
                      (+ 3)))
             (is= (-> state
                      (add-minion-to-board {:player-id "p1"
                                            :minion (create-minion "Malygos")
                                            :position 0})
                      (rampage "p1" "m1")
                      (get-minion "m1")
                      (:health))
                  (-> (get-definition "War Golem")
                      (:health)
                      (+ 8)))))}
  [state owner minion-id]
  (let [minion (get-minion state minion-id)
        minions (get-minions state owner)
        spell-damage (reduce
                      (fn [acc minion]
                        (let [spell (:spell-damage (get-definition minion))]
                          (if spell
                            (+ spell acc)
                            acc)))
                      0 minions)]
    (-> state
        (increase-attack (+ spell-damage 3) minion)
        (increase-health (+ spell-damage 3) minion))))

(defn coin
  "Gain 1 Mana Crystal this turn only."
  {:test (fn []
           (let [state (create-game [{:minions ["Lorewalker Cho"]
                                      :hand    ["The Coin"]}])]
             (is= (enough-mana? state "p1" 2)
                  false)
             (is= (-> (add-mana state 1 "p1")
                      (enough-mana? "p1" 2))
                  false)
             (is= (-> (coin state "p1")
                      (enough-mana? "p1" 2))
                  true)
             (is= (-> (coin state "p1")
                      (coin "p1")
                      (enough-mana? "p1" 3))
                  true)))}
  [state owner-id & minion-id]
  (-> (buff-entity-this-turn state owner-id :max-mana
                             (fn [m] (if (nil? m) 1 (inc m))))
      (add-mana 1 "p1")))