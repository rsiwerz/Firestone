(ns firestone.core
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.collections :refer [seq-contains?]]
            [ysera.error :refer [error]]
            [firestone.definitions :refer [get-definition]]
            [firestone.construct :refer [create-card
                                         create-game
                                         create-hero
                                         create-minion
                                         get-heroes
                                         get-hero
                                         get-minion
                                         get-minions
                                         get-player
                                         get-players
                                         player-id?
                                         update-minion]]))

(defn hero-power
  [hero]
  {:pre [(some? hero)]
   :post [some?]}
  (get-definition (:hero-power (get-definition hero))))

(defn get-character
  "Returns the character with the given id from the state.
   Passing a player-id yields that players hero."
  {:test (fn []
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-character "h1")
                    (:name))
                "Jaina Proudmoore")
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-character "p1")
                    (:name))
                "Jaina Proudmoore")
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-character "i")
                    (:name))
                "Imp"))}
  [state id]
  {:pre  [(or (string? id)
              (number? id))
          (some? state)]
   :post [(some? (:owner-id %))]}
  (if (player-id? state id)
    (get-hero state id)
    (->> (concat (get-minions state)
                 (get-heroes state))
         (filter (fn [c] (= (:id c) id)))
         (first))))

(defn get-character-states
  "Returning the states that a character is in, frozen, stealth etc."
  {:test (fn []
           (let [state (-> (create-game [{:minions [(create-minion "Blood Imp" :id "bi")]}]))]
             (is= (get-character-states state "bi")
                  ["STEALTH"]))
           (let [state (create-game [{:minions [(create-minion "Imp" :id "i")]}])]
             (is= (get-character-states state "i")
                  [])))}
  ([character]
   {:pre [(map? character)]}
   (if (nil? (:char-states character))
     (empty [])
     (:char-states character)))
  ([state character-id]
   {:pre [(map? (get-character state character-id))]}
   (get-character-states (get-character state character-id))))

(defn player-attribute
  "Any buffs are also added to the value"
  [state attribute player-id]
  (let [val (get-in state [:players player-id attribute])
        buff (get-in state [:turn :buffs player-id attribute])]
    (if (and (some? buff)
             (number? val)
             (number? buff))
      (+ buff val)
      val)))

(defn get-deck
  {:test (fn []
           (is= (-> (get-deck
                     (create-game)
                     "p1"))
                [])
           (is= (-> (get-deck
                     (create-game)
                     "p2"))
                []))}
  [state player-id]
  (player-attribute state :deck player-id))

(defn get-hand
  [state player-id]
  (player-attribute state :hand player-id))

(defn get-minion-from-hand
  [state player-id minion-id]
  (->> (get-hand state player-id)
       (filter (fn [minion] (= (:id minion) minion-id)))
       (first)))

(defn get-minion-from-deck
  [state player-id minion-id]
  (->> (get-deck state player-id)
       (filter (fn [minion] (= (:id minion) minion-id)))
       (first)))

(defn get-mana
  "Returns the mana from the player"
  {:test (fn []
           (is= (-> (create-game)
                    (get-mana "p1"))
                1))}
  [state player-id]
  (player-attribute state :mana player-id))

(defn get-max-mana
  "Returns the max-mana from the player"
  {:test (fn []
           (is= (-> (create-game)
                    (get-max-mana "p1"))
                1))}
  [state player-id]
  (player-attribute state :max-mana player-id))

(defn enough-mana?
  {:test (fn []
           (is (let [player-id "p1"
                     mana-cost 1]
                 (as-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}]) $
                       (enough-mana? $ player-id mana-cost))))
           (is (let [player-id "p1"
                     mana-cost 2]
                 (as-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}]) $
                       (assoc-in $ [:players player-id :mana] 10)
                       (enough-mana? $ player-id mana-cost))))
           (is-not (let [player-id "p1"
                         mana-cost 5]
                     (as-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}]) $
                           (enough-mana? $ player-id mana-cost))))
           (is (let [state (-> (create-game)
                               (assoc-in [:players "p1" :hand] (list {:name "Snake Trap", :entity-type :card, :id 1}
                                                                     {:name "Snake Trap", :entity-type :card, :id 2}
                                                                     {:name "Bananas", :entity-type :card, :id 3}))
                               (assoc-in [:players "p1" :secrets] (list))
                               (assoc-in [:players "p1" :mana] 10))]
                 (enough-mana? state "p1" 1)))
           (is= (-> (create-game)
                    (enough-mana? "p1" 0))
                true))}
  [state player-id mana-cost]
  (if (nil? (get-mana state player-id))
    (as-> (error "Warning: get-mana returned nil, the player may not have a mana key.") $
          false)
    (>= (get-mana state player-id) mana-cost)))

(defn reduce-mana
  {:test (fn []
           (is= (as-> (create-game [{:hero "Jaina Proudmoore"} {:hero "Anduin Wrynn"}]) $
                      (player-attribute $ :mana "p1"))
                1)
           (is= (as-> (create-game [{:hero "Jaina Proudmoore"} {:hero "Anduin Wrynn"}]) $
                      (reduce-mana $ "p1" 1)
                      (player-attribute $ :mana "p1"))
                0))}
  [state player-id amount]
  (-> (if (contains? (get-player state player-id) :mana)
        state
        (assoc-in state [:players player-id :mana] 1))
      (update-in [:players player-id :mana] (fn [x] (- x amount)))))

(defn add-mana
  {:test (fn []
           (is= (-> (create-game)
                    (add-mana 4 "p1")
                    (get-mana "p1"))
                1)
           (is= (-> (create-game)
                    (assoc-in [:players "p1" :max-mana]  7)
                    (add-mana 4 "p1")
                    (get-mana "p1"))
                5)
           (is= (-> (create-game)
                    (assoc-in [:players "p1" :max-mana]  10)
                    (assoc-in [:players "p1" :mana]  7)
                    (add-mana 4 "p1")
                    (get-mana "p1"))
                10))}
  [state mana player-id]
  (let [max-mana (get-max-mana state player-id)]
    (update-in state [:players player-id :mana] (fn [m] (min (+ m mana)
                                                             max-mana)))))
(defn add-max-mana
  {:test (fn []
           (is= (-> (create-game)
                    (add-max-mana 7 "p1")
                    (get-max-mana "p1"))
                8)
           (is= (-> (create-game)
                    (add-max-mana 11 "p1")
                    (get-max-mana "p1"))
                10))}
  [state mana player-id]
  (update-in state [:players player-id :max-mana] (fn [m] (min (+ m mana)
                                                               10))))
(defn get-health
  "Returns the health of the character."
  {:test (fn []
           ; The health of minions
           (is= (get-health (create-minion "War Golem")) 7)
           (is= (get-health (create-minion "War Golem" :damage-taken 2)) 5)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-health "i"))
                1)
           ; The health of heroes
           (is= (get-health (create-hero "Jaina Proudmoore")) 30)
           (is= (get-health (create-hero "Jaina Proudmoore" :damage-taken 2)) 28)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-health "h1"))
                30))}
  ([character]
   {:pre [(map? character) (contains? character :damage-taken)]}
   (let [card-def (get-definition character)]
     (if (contains? character :health)
       (- (:health character) (:damage-taken character))
       (- (:health card-def) (:damage-taken character)))))
  ([state id]
   (get-health (get-character state id))))

(defn get-max-health
  "Returns the max health of the character."
  {:test (fn []
           ; The health of minions
           (is= (get-max-health (create-minion "War Golem")) 7)
           (is= (get-max-health (create-minion "War Golem" :damage-taken 2)) 7)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-max-health "i"))
                1)
           ; The health of heroes
           (is= (get-max-health (create-hero "Jaina Proudmoore")) 30)
           (is= (get-max-health (create-hero "Jaina Proudmoore" :damage-taken 2)) 30)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1")}])
                    (get-max-health "h1"))
                30)
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1" :bonus-health 2)}])
                    (get-max-health "h1"))
                32))}
  ([character]
   {:pre [(map? character)]}
   (let [definition (get-definition character)]
     (if (contains? character :bonus-health)
       (+ (:health definition) (:bonus-health character))
       (:health definition))))

  ([state id]
   (get-max-health (get-character state id))))

(defn get-attack
  "Returns the attack of the minion with the given id."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-attack "i"))
                1))}
  [state id]
  {:post [(some? %)]
   :pre [(or (string? id)
             (number? id))]}
  (:attack (get-minion state id)))

(defn sleepy?
  "Checks if the minion with given id is sleepy."
  {:test (fn []
           (is (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                :minion-ids-summoned-this-turn ["i"])
                   (sleepy? "i")))
           (is-not (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                       (sleepy? "i"))))}
  [state id]
  (seq-contains? (:minion-ids-summoned-this-turn state) id))

(defn stealth?
  "Checks if the minion with the given ID is in stealth"
  {:test (fn []
           (is (-> (create-game [{:minions [(create-minion "Blood Imp" :id "bi")]}])
                   (stealth? "bi")))
           (is-not (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                       (stealth? "i"))))}
  [state minion-id]
  (as-> (get-minion state minion-id) $
        (seq-contains? (get-in $ [:char-states]) "STEALTH")))

(defn frozen?
  "Checks if the minion with the given ID is frozen"
  [state minion-id]
  (as-> (get-minion state minion-id) $
        (seq-contains? (get-in $ [:char-states]) "FROZEN")))



(defn have-used-hero-power-this-turn?
  [state player-id]
  (if (contains? state :turn)
    (->> state
         :turn
         :event-log
         (filter (fn [e] (and (= (:event-type e)
                                 :use-hero-power)
                              (= (:player-id e) player-id))))
         (empty?)
         (not))
    false))

(defn can-use-hero-power?
  {:test (fn []
           ; Can only attack once per turn
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type :use-hero-power, :player-id "p1", :target-id "h2"}]}
                                 :player-id-in-turn "p1")
                    (assoc-in [:players "p1" :mana] 100)
                    (can-use-hero-power? "p1"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type :something}
                                                    {:event-type :use-hero-power, :player-id "p1", :target-id "h2"}]}
                                 :player-id-in-turn "p1")
                    (assoc-in [:players "p1" :mana] 100)
                    (can-use-hero-power? "p1"))
                false)
           ; Can only attack if it is your turn
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :player-id-in-turn "p2")
                    (assoc-in [:players "p1" :mana] 100)
                    (can-use-hero-power? "p1"))
                false)
           ; Can only attack if we have enough mana.
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :player-id-in-turn "p1")
                    (assoc-in [:players "p1" :mana] 0)
                    (can-use-hero-power? "p1"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type :damage-hero, :player-id "h2", :damage 1}]}
                                 :player-id-in-turn "p1")
                    (assoc-in [:players "p1" :mana] 100)
                    (can-use-hero-power? "p1"))
                true)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :player-id-in-turn "p1")
                    (assoc-in [:players "p1" :mana] 100)
                    (can-use-hero-power? "p1"))
                true))}
  [state player-id]
  (let [player      (get-player state player-id)
        hero        (get player :hero)
        hero-power  (hero-power (:name hero))]
    (and (enough-mana? state player-id (:mana-cost hero-power))
         (= player-id (:player-id-in-turn state))
         (not (have-used-hero-power-this-turn? state player-id)))))


(defn attacks-performed-this-turn
  "return the numbers of time a hero or minion have attacked this turn"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (attacks-performed-this-turn "i"))
                0)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}]})
                    (attacks-performed-this-turn "i"))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}
                                                    {:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}
                                                    {:event-type  :attack-hero
                                                     :attacker-id "i"
                                                     :target-id   "target"}]})
                    (attacks-performed-this-turn "i"))
                3)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}
                                                    {:event-type  :attack-minion
                                                     :attacker-id "j"
                                                     :target-id   "target"}
                                                    {:event-type  :attack-hero
                                                     :attacker-id "j"
                                                     :target-id   "target"}
                                                    {:event-type :draw-card
                                                     :player-id "p1"}]})
                    (attacks-performed-this-turn "i"))
                1))}
  [state character-id]
  (if (contains? state :turn)
    (->> state
         :turn
         :event-log
         (filter (fn [e] (and
                           (or
                             (= (:event-type e)
                                :attack-minion)
                             (= (:event-type e)
                                :attack-hero))
                           (= (:attacker-id e) character-id))))
         (count))
    0))

(defn spells-casted-this-turn?

  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type :play-card
                                                     :card-type :spell}]})
                    (spells-casted-this-turn?))
                true)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                 :turn {:event-log [{:event-type :play-card
                                                     :card-type :minion}]})
                    (spells-casted-this-turn?))
                false))}
  [state]
  (if (contains? state :turn)
    (->> state
         :turn
         :event-log
         (filter (fn [e] (and (= (:event-type e)
                                 :play-card)
                              (= (:card-type e)
                                 :spell))))
         (empty?)
         (not))
    false))

(defn can-attack?
  "Checks if a character can attack"
  {:test (fn []
           ; Minion should not be able to attack if you are sleepy
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                    :minion-ids-summoned-this-turn ["i"])
                       (can-attack? "i"))
                false)
           ; Minion should not be able to attack if it already attacked this turn
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                    :turn {:event-log [{:event-type  :attack-minion
                                                        :attacker-id "i"
                                                        :target-id   "target"}]})
                       (can-attack? "i"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]
                                    :turn {:event-log [{:event-type  :attack-hero
                                                        :attacker-id "i"
                                                        :target-id   "target"}]})
                       (can-attack? "i"))
                false)
           ;; Minions with windfury should be able to attack twice
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i"
                                                            :windfury true
                                                            )]}]
                                 :turn {:event-log [{:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}]})
                    (can-attack? "i"))
                true)
           ;; Minions with windfury should not be able to attack more than twice
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i"
                                                            :windfury true)]}]
                                 :turn {:event-log [{:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}
                                                    {:event-type  :attack-minion
                                                     :attacker-id "i"
                                                     :target-id   "target"}]})
                    (can-attack? "i"))
                false)
           ; Minion should not be able to attack if :cant-attack for the attacker is true
           (is-not (-> (create-game [{:minions [(create-minion "Ancient Watcher" :id "i")]}])
                       (can-attack? "i")))
           ; Minion should be able to attack if :cant-attack for the attacker is false
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (can-attack? "i"))
                true)
           ;; Minions without attack shouldn't be able to attack
           (is= (-> (create-game [{:minions ["Shieldbearer"]}])
                    (can-attack? "m1"))
                false)
           ;; Heros should never be able to attack.
           (is= (-> (create-game [{:hero (create-hero "Jaina Proudmoore" :id "h1" :cant-attack false :owner-id "p1")}])
                    (can-attack? "h1"))
                false)
           ;;Frozen characters should not be able to attack
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (update-minion "i" :char-states ["FROZEN"])
                    (can-attack? "i"))
                false)
           ;; Unpowered Mauler should not be able to attack if spell not casted
           (is= (-> (create-game [{:minions [(create-minion "Unpowered Mauler" :id "um")]}])
                    (can-attack? "um"))
                false)
           ;; Unpowered Mauler should be able to attack if spell has been casted
           (is= (-> (create-game [{:minions [(create-minion "Unpowered Mauler" :id "um")]}]
                                 :turn {:event-log [{:event-type :play-card
                                                     :card-type :spell}]})
                    (can-attack? "um"))
                true))}
  [state character-id]
  {:pre  [(or (string? character-id)
              (number? character-id))]}
  (let [character (get-character state character-id)]
    (and (not (:cant-attack character))
         (= (:player-id-in-turn state) (:owner-id character))
         (not (sleepy? state character-id))
         (some? (:attack character))
         (< 0 (:attack character))
         (not (frozen? state character-id))
         (if (= (:name character) "Unpowered Mauler")
           (spells-casted-this-turn? state)
           true)
         (or (< (attacks-performed-this-turn state (:id character)) 1)
             (if (contains? character :windfury)
               (and (:windfury character)
                    (< (attacks-performed-this-turn state (:id character)) 2))
               false)))))

(defn exist-taunt?
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "s")]}
                                  {:minions [(create-minion "War Golem" :id "wg")]}])
                    (exist-taunt? "p1"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "s")]}
                                  {:minions [(create-minion "War Golem" :id "wg")]}])
                    (exist-taunt? "p2"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Shieldbearer" :id "s")]}
                                  {:minions [(create-minion "War Golem" :id "wg")]}])
                    (exist-taunt? "p1"))
                true)
           (is= (-> (create-game [{:minions [(create-minion "Shieldbearer" :id "s")]}
                                  {:minions [(create-minion "War Golem" :id "wg")]}])
                    (exist-taunt? "p2"))
                false)
           (is= (-> (create-game [{:minions [(create-minion "Shieldbearer" :id "s")
                                             (create-minion "War Golem" :id "wg")
                                             (create-minion "Shieldbearer" :id "s2")]}])
                    (exist-taunt? "p1"))
                true))}
  [state player-id]
  (->> (get-minions state player-id)
       (filter :taunt)
       (empty?)
       (not)))

(defn valid-attack?
  "Checks if the attack is valid"
  {:test (fn []
           ; Should be able to attack an enemy minion
           (is (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}
                                 {:minions [(create-minion "War Golem" :id "wg")]}])
                   (valid-attack? "i" "wg")))
           ; Should be able to attack an enemy hero
           (is (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                   (valid-attack? "i" "h2")))
           ; Should not be able to attack your own minions
           (is-not (-> (create-game [{:minions [(create-minion "Imp" :id "i")
                                                (create-minion "War Golem" :id "wg")]}])
                       (valid-attack? "i" "wg")))
           ; Should not be able to attack if it is not your turn
           (is-not (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}
                                     {:minions [(create-minion "War Golem" :id "wg")]}]
                                    :player-id-in-turn "p2")
                       (valid-attack? "i" "wg")))
           ; Should not be able to attack if target is in stealth
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}
                                  {:minions [(create-minion "Moroes" :id "m")]}]
                                 :player-id-in-turn "p1")
                    (valid-attack? "i" "m"))
                false)
           ; Should only be able to attack minions with taunt
           (is-not (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}
                                     {:minions [(create-minion "War Golem" :id "wg")
                                                (create-minion "Shieldbearer" :id "s")]}]
                                    :player-id-in-turn "p1")
                       (valid-attack? "i" "wg")))
           (is (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}
                                     {:minions [(create-minion "War Golem" :id "wg")
                                                (create-minion "Shieldbearer" :id "s")]}]
                                    :player-id-in-turn "p1")
                       (valid-attack? "i" "s"))))}

  [state attacker-id target-id]
  {:pre [(or (string? attacker-id)
             (number? attacker-id))
         (or (string? target-id)
             (number? target-id))
         (map? state)]}
  (let [attacker (get-minion state attacker-id)
        target (get-character state target-id)]
    (and (can-attack? state attacker-id)
         (not= (:owner-id attacker) (:owner-id target))
         (not (stealth? state target-id))
         (if (exist-taunt? state (:owner-id target))
           (:taunt target)
           true))))

(defn playable?
  [state player-id card-def]
  (and (enough-mana? state player-id (:mana-cost card-def))
       (if (= (:type card-def) :minion)
         (< (count (get-minions state player-id)) 7)
         true)))

(defn heal-character
  "Heals the character for amount :amount"
  {:test (fn []
           (let [state (create-game [{:hero (create-hero "Jaina Proudmoore" :damage-taken 3)}
                                     {:minions [(create-minion "Imp") (create-minion "War Golem" :id "w1" :damage-taken 4)]}])]
             (is= (-> state
                      (heal-character "h1" 2)
                      (get-in [:players "p1" :hero])
                      (:damage-taken))
                  1)
             (is= (-> state
                      (heal-character "w1" 2)
                      (get-minion "w1")
                      (:damage-taken))
                  2)))}
  [state id amount]
  (let [character (get-character state id)
        character-type (:entity-type character)
        curr-health (:damage-taken character)]
    (if (= character-type :hero)
      (assoc-in state [:players (:owner-id character) character-type :damage-taken] (- curr-health amount))
      (update-minion state id :damage-taken (- curr-health amount)))))
