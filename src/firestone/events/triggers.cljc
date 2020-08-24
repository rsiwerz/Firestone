(ns firestone.events.triggers
  (:require [firestone.definition.minion-modifiers :refer [increase-attack
                                                           increase-health]]
            [ysera.test :refer [is is-not is= error?]]
            [ysera.random :refer [random-nth
                                  shuffle-with-seed]]
            [firestone.core :refer [get-hand
                                    get-health]]
            [firestone.construct :refer [add-card-to-hand
                                         add-minion-to-board
                                         create-card
                                         create-game
                                         create-minion
                                         create-secret
                                         get-opponent-id
                                         get-minion
                                         get-minions
                                         get-players
                                         generate-id
                                         get-seed
                                         set-seed
                                         remove-minion
                                         replace-minion
                                         update-minion
                                         remove-secret
                                         get-secrets]]
            [firestone.events.queue :refer [enqueue-event
                                            peek-event
                                            get-events]]))

(defn frothing-berserker
  "Whenever a minion takes damage, gain +1 Attack."
  [event self state]
  (if (= (:event-type event)
         :damage-minion)
    (increase-attack state 1 self)
    state))

(defn competitive-spirit-secret
  "Secret: When your turn starts give your minions +1/+1."
  {:test (fn []
           (let [secret (create-secret "Competitive Spirit" :owner-id "p1" :id "c")
                 state (-> (create-game [{:minions ["Imp" "War Golem"]
                                          :secrets [secret]}])
                           (enqueue-event :start-of-turn "p1"))
                 event (peek-event state)]
             (is= (->> state
                       (competitive-spirit-secret event secret)
                       (get-minions)
                       (map :attack))
                  [2 8])
             (is= (->> state
                       (competitive-spirit-secret event secret)
                       (get-minions)
                       (map :damage-taken))
                  [-1 -1])
             (is= (-> (competitive-spirit-secret event secret state)
                      (get-secrets "p1"))
                  [])))}
  [event self state]
  (let [owner (:owner-id self)
        secret-id (:id self)
        minions (get-minions state owner)]
    (if (and
         (= (:player-id-in-turn state) owner)
         (= (:event-type event) :start-of-turn)
         (not (empty? minions)))
      (-> (reduce (fn [state {attack :attack
                         health :damage-taken
                         minion-id :id}]
                (-> (update-minion state minion-id :attack inc)
                    (update-minion minion-id :damage-taken dec)))
              state minions)
          (remove-secret secret-id))
      state)))

(defn snake-trap-secret
  "Secret: When one of your minions is attacked summon three 1/1 Snakes."
  {:test (fn []
           (let [secret {:entity-type :secret, :name "Snake Trap", :id "c3", :owner-id "p2"}
                 event {:event-type  :attack-minion
                        :attacker-id 1
                        :target-id   2}
                 state (-> (create-game
                             [{:minions [(create-minion "Imp" :id 1)]
                               :secrets [secret]}
                              {:minions [(create-minion "War Golem" :id 2)]}]))]
             (is= (->> state
                       (snake-trap-secret event secret)
                       (get-events))
                  (repeat 3 {:event-type  :summon-minion
                                :player-id   "p2"
                                :minion-name "Snake"}))
             (is= (-> (competitive-spirit-secret event secret state)
                      (get-secrets "p2"))
                  [])))}
  [event self state]
  (let [target (get-minion state (:target-id event))
        secret-id (:id self)]
    (if (and
          (= (:event-type event)
             :attack-minion)
          (= (:owner-id target)
             (:owner-id self))
          (> 7 (count (get-minions state (:owner-id self)))))
      (-> (reduce enqueue-event state
              (repeat 3 {:event-type  :summon-minion
                            :player-id   (:owner-id self)
                            :minion-name "Snake"}))
          (remove-secret secret-id))
      state)))

(defn acolyte-of-pain
  "Whenever this minion takes damage, draw a card."
  {:test (fn []
           (let [event {:event-type :damage-minion
                        :minion-id  "m1"
                        :damage     2}
                 state (create-game
                         [{:minions ["Acolyte of Pain"]}])
                 minion (get-minion state "m1")]
             (is= (-> (acolyte-of-pain event minion state)
                      (get-events))
                  [{:event-type :draw-card
                    :player-id  "p1"}])
             (let [state (acolyte-of-pain event minion state)
                   event (-> (get-events state)
                             (first))]
               (is= (-> (acolyte-of-pain event minion state)
                        (get-events))
                    [{:event-type :draw-card
                      :player-id  "p1"}]))))}
  [event self state]
  (if (and (= (:event-type event)
              :damage-minion)
           (= (:minion-id event)
              (:id self)))
    (enqueue-event state :draw-card (:owner-id self))
    state))

(defn moroes
  "Stealth: At the end of your turn, summon a 1/1 Steward.
  Listens for end-turn event that belongs to the player.
  Summons a Steward."
  {:test (fn []
           (let [event {:event-type :end-turn}
                 state (create-game
                         [{:minions ["Moroes"]}])
                 minion (get-minion state "m1")]
             (is= (-> (moroes event minion state)
                      (get-events))
                  [{:event-type :summon-minion
                    :player-id (:owner-id minion)
                    :minion-name "Steward"}])))}
  [event self state]
  (if (and (= (:event-type event)
              :end-turn)
           (= (:player-id-in-turn state)
              (:owner-id self)))
    (enqueue-event state :summon-minion (:owner-id self) "Steward")
    state))

(defn blood-imp
  "Stealth. At the end of your turn, give another random friendly minion +1 Health.
  Listens for end-turn event that belongs to the player.
  Filters all minions on the own player side. Chooses one at random to buff."
  {:test (fn []
           (let [event {:event-type :end-turn}
                 state (-> (create-game [{:minions ["Blood Imp" "Imp"]}
                                         {:minions ["Blood Imp" "War Golem"]}])
                           (set-seed 1))
                 minion (get-minion state "m1")]

             (is= (as-> state $
                        (blood-imp event minion $)
                        (get-minion $ "m2")
                        (get-health $))
                  2)
             (is-not (= (->> state
                             (blood-imp event minion)
                             (get-seed))
                        1))))}
  [event self state]
  (if (and (= (:event-type event)
              :end-turn)
           (= (:player-id-in-turn state)
              (:owner-id self)))
    (let [all-minions (get-minions state (:owner-id self))
          other-minions (filter (fn [x]
                                  (not= (:id x) (:id self))) all-minions)
          [newseed shuffled-minions] (shuffle-with-seed (get-seed state) other-minions)
          random-minion (first shuffled-minions)]
      (if (some? random-minion)
        (-> (set-seed state newseed)
            (increase-health 1 random-minion))
        state))
    state))

(defn lorewalker-cho
  "Whenever a player casts a spell, put a copy into the other player's hand."
  {:test (fn []
           (let [state (create-game)
                 event {:event-type :play-card
                        :card-type  :secret
                        :player-id  "p1"
                        :card       {:name      "Snake Trap"
                                     :card-type :spell
                                     :secret    true}}]
             (is= (lorewalker-cho event nil state)
                  (add-card-to-hand state "p2" "Snake Trap"))))}
  [event self state]
  {:post [some?]}
  (if (and (= (:event-type event) :play-card)
           (or (= (:card-type event) :spell)
               (= (:card-type event) :secret)))
    (let [caster (:player-id event)
          other-player (-> (:players state)
                           (dissoc caster)
                           (keys)
                           (first))
          card-name (:name (:card event))]
      (add-card-to-hand state other-player card-name))
    state))

(defn archmage-antonidas
  "Whenever you cast a spell, put a 'Fireball' spell into your hand."
  {:test (fn []
           (let [state (create-game [{:minions ["Archmage Antonidas"]}])
                 event {:event-type :play-card
                        :card-type  :secret
                        :player-id  "p1"
                        :card       {:name      "Snake Trap"
                                     :card-type :spell
                                     :secret    true}}]
             (is= (archmage-antonidas event (get-minion state "m1") state)
                  (add-card-to-hand state "p1" "Fireball"))))}
  [event {owner :owner-id} state]
  {:post [some?]}
  (if (and (= (:event-type event) :play-card)
           (or (= (:card-type event) :spell)
               (= (:card-type event) :secret))
           (= (:player-id event) owner))
    (add-card-to-hand state owner "Fireball")
    state))

(defn doomsayer
  "At the start of your turn destroy ALL minions."
  {:test (fn []
           (let [event {:event-type :start-of-turn}
                 state (create-game [{:minions ["Imp" "Doomsayer" "War Golem"]}])
                 minion (get-minion state "m2")]
             (is= (-> (doomsayer event minion state)
                      (get-events))
                  [{:event-type :destroy-minion
                    :player-id "p1"
                    :minon-id "m1"}
                   {:event-type :destroy-minion
                    :player-id "p1"
                    :minon-id "m2"}
                   {:event-type :destroy-minion
                    :player-id "p1"
                    :minon-id "m3"}])))}
  [event self state]
  (let [minions (get-minions state)]
    (if (= (:event-type event) :start-of-turn)
      (reduce (fn [state {minion-id :id owner :owner-id}]
                (enqueue-event state {:event-type :destroy-minion
                                      :player-id owner
                                      :minon-id minion-id})) state minions)
      state)))

(defn alarm-o-bot
  "At the start of your turn swap this minion with a random one in your hand."
  {:test (fn []
           (let [event {:event-type :start-of-turn}
                 state (create-game [{:minions ["Alarm-o-Bot"]
                                      :hand ["Imp" "War Golem" "Arcane Golem"]}])
                 minion (get-minion state "m1")]
             (is= (as-> (set-seed state 2) $
                    (alarm-o-bot event minion $)
                    (get-minions $)
                    (map :name $)
                    (first $))
                  "Arcane Golem")
             (is= (as-> (set-seed state 2) $
                    (alarm-o-bot event minion $)
                    (get-hand $ "p1")
                    (map :name $))
                  ["Imp" "War Golem" "Alarm-o-Bot"])))}
  [event self state]
  (if (= (:event-type event) :start-of-turn)
    (let [owner (:player-id-in-turn state)
          seed (get-seed state)
          hand (get-hand state owner)
          [new-seed random-minion] (random-nth seed hand)
          self-id (:id self)
          [state new-id] (generate-id state)
          replaced-minion (create-minion (:name random-minion) :id self-id :owner-id owner)]
      (-> (update-in state [:players owner :hand]
                     (fn [cards]
                       (map (fn [card]
                              (if (= (:id random-minion) (:id card))
                                (create-card (:name self) :id (str "c" new-id))
                                card))
                            cards)))
          (replace-minion replaced-minion)
          (set-seed new-seed)))
    state))


(defn gallywix
  "Whenever your opponent casts a spell, gain a copy of it and give them a Coin."
  [event self state]
  (let [player-id (:owner-id self)
        opponent-id (get-opponent-id state player-id)]
    (if (and (= (:event-type event)
                :play-card)
             (or (= (:card-type event)
                    :spell)
                 (= (:card-type event)
                    :secret))
             (= (:player-id event)
                opponent-id)
             (not= (:name (:card event))
                   "Gallywix's Coin"))
      (-> state
          (add-card-to-hand opponent-id (create-card "Gallywix's Coin"))
          (add-card-to-hand player-id (create-card (:name (:card event)))))
      state)))

(defn lightwarden
  [event self state]
    (if (= (:event-type event)
           :restore-health)
      (increase-attack state 2 self)
      state))