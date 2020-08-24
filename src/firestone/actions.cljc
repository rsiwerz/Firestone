(ns firestone.actions
  (:require [clojure.test :refer [function?]]
            [ysera.test :refer [is is-not is= error?]]
            [ysera.random :refer [shuffle-with-seed]]
            [firestone.construct :refer [create-game
                                         get-minion
                                         get-minions
                                         get-heroes
                                         get-opponent-id
                                         create-empty-state
                                         create-hero
                                         create-minion
                                         create-card
                                         create-secret
                                         update-minion
                                         update-hero
                                         add-minion-to-board
                                         get-player
                                         get-players
                                         replace-minion
                                         remove-minion
                                         get-seed
                                         set-seed
                                         add-secret]]
            [firestone.definitions :refer [get-definition]]
            [firestone.definition.card :refer [card-definitions]]
            [firestone.definition.hero]
            [firestone.core :refer [valid-attack?
                                    get-attack
                                    get-health
                                    player-attribute
                                    get-deck
                                    get-hand
                                    get-minion-from-hand
                                    get-minion-from-deck
                                    get-mana
                                    get-max-mana
                                    get-character
                                    hero-power
                                    reduce-mana
                                    add-max-mana
                                    add-mana
                                    frozen?
                                    attacks-performed-this-turn]]
            [firestone.util :refer [irange
                                    printerr]]
            [firestone.events.queue :refer [enqueue-event
                                            get-events]]
            [firestone.definition.minion-modifiers :refer [increase-attack
                                                           remove-character-state]]))

(declare
  init-game
  end-turn
  play-minion-card
  attack-minion)

(defn get-turn-counter
  {:test (fn []
           (is= (-> (create-game)
                    (assoc-in [:turn-counter] 5)
                    (get-turn-counter))
                5)
           (is= (-> (create-game)
                    (get-turn-counter))
                0)
           )}
  [state]
  (if (contains? state :turn-counter)
    (get-in state [:turn-counter])
    (-> (assoc-in state [:turn-counter] 0)
        (get-turn-counter))))

(defn increase-turn-counter
  {:test (fn []
           (is= (-> (init-game ["Jaina Proudmoore", "Anduin Wrynn"])
                    (get-in [:turn-counter]))
                0)
           (is= (-> (init-game ["Jaina Proudmoore", "Anduin Wrynn"])
                    (increase-turn-counter)
                    (get-in [:turn-counter]))
                1)
           (is= (as-> (init-game ["Jaina Proudmoore", "Anduin Wrynn"]) $
                      (end-turn $ "p1")
                      (end-turn $ "p2")
                      (get-in $ [:turn-counter]))
                2))}
  [state]
  (let [current-turn-counter (get-turn-counter state)]
    (update-in state [:turn-counter] (fn [x] (+ current-turn-counter 1)))))

(defn add-minion-to-hand
  "Add a minion with name :minion to state :state owned by player :player-id"
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp")]}])
                    (add-minion-to-hand "p1" "Big Game Hunter")
                    (get-minion-from-hand "p1" "m0")
                    (:name))
                "Big Game Hunter"))}
  [state player-id minion]
  (let [nr-of-minions (count (:minions state))
        minion (create-minion minion :id (str "m" nr-of-minions))]
    (update-in state [:players player-id :hand]
               (fn [minions]
                 (conj minions minion)))))

(defn fill-deck
  [deck]
  (as-> card-definitions $
    (keys $)
    (concat $ $)
    (map create-card $)
    (take 30 $)
    (concat deck $)))

(defn assign-id [pred cards]
    (map
      (fn [card i] (assoc card :id (str i)))
      cards
      (filter pred (irange 1 60))))

(defn damage-hero
  {:test (fn []
           (is= (let [state (init-game ["Jaina Proudmoore", "Anduin Wrynn"])]
                  (-> (damage-hero state "p1" 5)
                      (get-in [:players "p1" :hero :damage-taken])))
                5))}
  [state id damage]
  {:pre [(number? damage)]}
  (update-hero state id :damage-taken (partial + damage)))

;; TODO: Implement tests
(defn attack-hero
  "Check if attack is valid given a target hero and an attacker. Then call damage-hero"
  [state player-id attacker-id]
  {:pre [(or (string? attacker-id)
             (number? attacker-id))
         (string? player-id)
         (map? state)]}
  (if (valid-attack? state attacker-id player-id)
    (-> (enqueue-event state :damage-hero player-id (get-attack state attacker-id)))
    (do (printerr "valid-attack returned false. Did the minion die?")
        state)))

(defn destroy-minion
  "Destroys the minion with id :id"
  {:test (fn []
           (let [player "p1"
                 minion (create-minion "War Golem" :id "w1")
                 state (create-game [{:minions [minion]}]
                                    :player-id-in-turn player)]
             (is= (-> state
                      (destroy-minion player (:id minion))
                      (get-minions player))
                  [])))}
  [state player minion-id]
  (remove-minion state minion-id))

(defn damage-minion
  "Damage the minion with the given id by the given damage."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (damage-minion "i" 1)
                    (get-minion "i")
                    (:damage-taken))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (damage-minion "i" 2)
                    (get-minion "i")
                    (:damage-taken))
                2)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (damage-minion "i" 2)
                    (get-events))
                (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (enqueue-event :destroy-minion "p1" "i")
                    (get-events))))}
  [state id damage]
  {:pre [(some? damage)]}
  (let [state (update-minion state id :damage-taken (partial + damage))
        minion (get-minion state id)]
    (if (<= (:health minion) (:damage-taken minion))
      (enqueue-event state
                     :destroy-minion
                     (:owner-id minion)
                     (:id minion))
      state)))

(defn attack-minion
  "Damages minion/hero with id - minion-id with the minion/hero with id - attacker-id"
  [state target-id attacker-id]
  (if (valid-attack? state attacker-id target-id)
    (let [attack (get-attack state attacker-id)]
      (assert (some? attack))
      (-> state
          (remove-character-state attacker-id "STEALTH")
          (enqueue-event :damage-minion target-id attack)
          (enqueue-event :damage-minion attacker-id (get-attack state target-id))))
    (println "Attack not valid")))

(defn overdraw [state n player-id]
  "Discarding a card in the case of overdrawing (<=10 cards in the hand)"
  (update-in state [:players player-id :deck] (partial drop n)))

(defn draw-card
  ;; TODO: Effects when drawing cards.
  {:test (fn []
           (is= (-> {:players {"p1"
                                 {:deck (irange 1 30) :hand (list)}
                               "p2"
                                 {:deck (irange 31 60) :hand (list)}}}
                    (draw-card 3 "p1")
                    (draw-card 4 "p2"))
                {:players {"p1"
                           {:deck (irange 4 30) :hand (irange 1 3)}
                           "p2"
                           {:deck (irange 35 60) :hand (irange 31 34)}}})
           (is= (-> {:players {:p {
                                   :deck (irange 3 6)
                                   :hand (irange 1 2)
                                   }}}
                    (draw-card 2 :p)
                    (get-in [:players :p :hand])
                    (sort))
                (irange 1 4))
           (is= (-> (init-game [])
                    (draw-card 5 "p1")
                    (get-in [:players "p1" :deck])
                    (count))
                (-> (init-game [])
                    (get-in [:players "p1" :deck])
                    (count)
                    (- 5)
                    (max 0)))
           (is= (-> (init-game [])
                    (draw-card 8 "p1")
                    (get-in [:players "p1" :deck])
                    (count))
                (-> (init-game [])
                    (get-in [:players "p1" :deck])
                    (count)
                    (- 8)
                    (max 0)))
           (is= (-> (init-game [])
                    (draw-card 20 "p1")
                    (get-in [:players "p1" :deck])
                    (count))
                (-> (init-game [])
                    (get-in [:players "p1" :deck])
                    (count)
                    (- 20)
                    (max 0))))}
  [state n player-id]
  (if (<= n 0) state
  ;; If the deck is empty, damage the hero according to rules of fatigue
  (if (empty? (get-in state [:players player-id :deck]))
    (as-> (update-in state [:players player-id :fatigue]
          (fn [fatigue]
            (if (nil? fatigue)
              0
              (inc fatigue)))) $
        (damage-hero $ player-id (get-in $ [:players player-id :fatigue])))

    ;;If a player has more than 10 cards in hand, call overdraw
    (if (>= (count (get-in state [:players player-id :hand])) 10)
      (overdraw state n player-id)

      ;;Else, add a card to the hand and remove the first card in the deck. Recursively call draw-card.
      (-> (update-in state [:players player-id]
            (fn [p]
              (assoc p
                :hand (conj (vec (:hand p)) (first (:deck p)))
                :deck (rest (:deck p)))))
          (draw-card (dec n) player-id))))))

(defn init-game
  {:test (fn []
           (is= (-> (init-game ["Jaina Proudmoore" "Rexxar"])
                    (get-mana "p1"))
                1)
           (is= (-> (init-game ["Jaina Proudmoore" "Anduin Wrynn"])
                    (get-deck "p1")
                    (count))
                27)
           (is= (-> (init-game ["Jaina Proudmoore" "Anduin Wrynn"])
                    (get-deck "p2")
                    (count))
                26)
           (is= (-> (init-game ["Jaina Proudmoore" "Anduin Wrynn"])
                    (get-hand "p1")
                    (count))
                3)
           (is= (-> (init-game ["Jaina Proudmoore" "Anduin Wrynn"])
                    (get-hand "p2")
                    (count))
                4)
           (is= (-> (init-game ["Jaina Proudmoore" "Anduin Wrynn"])
                    (get-minions)
                    (empty?))
                true)
           (let [state (init-game ["Jaina Proudmoore","Anduin Wrynn"])
                 cards (map :name (concat
                                   (-> state
                                       (get-deck "p1"))
                                   (-> state
                                       (get-deck "p2"))
                                   (-> state
                                       (get-hand "p1"))
                                   (-> state
                                       (get-hand "p2"))))]
             (is= cards
                  (filter (fn [x] (contains? card-definitions x)) cards)))
           )}
  [heroes]
  (let [state (create-game (vec (map (fn [hero] (assoc {} :hero (create-hero hero))) heroes)))
        [p1 p2] (keys (:players (create-game)))]
    ;; Fill the players deck
    (-> (update-in state [:players p1 :deck] fill-deck)
        (update-in [:players p2 :deck] fill-deck)
        ;; Assign id:s to minions
        (update-in [:players p1 :deck] (partial assign-id odd?))
        (update-in [:players p2 :deck] (partial assign-id even?))
        ;; Set seed and turn-counter
        (set-seed 1)
        (assoc-in [:turn-counter] 0)
        ;; Fill mana
        (assoc-in [:players p1 :mana] 1)
        (assoc-in [:players p2 :mana] 1)
        (assoc-in [:minion-ids-summoned-this-turn] [])
        (assoc-in [:players p1 :secrets] [])
        (assoc-in [:players p2 :secrets] [])
        (draw-card 3 p1)
        (draw-card 4 p2)
        (assoc-in [:players p1 :hero :owner-id] p1)
        (assoc-in [:players p2 :hero :owner-id] p2))))


(defn remove-card
  "Remove the card from the players hand"
  {:test (fn []
           (is= (let [state (create-game [{:hand ["Imp" "Big Game Hunter" "War Golem"]}])
                      card (first (get-in state [:players "p1" :hand]))]
                  (-> (remove-card state "p1" card)
                      (get-in [:players "p1" :hand])
                      (first)
                      (:name)))
                "Big Game Hunter")
           (is= (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
                      card (first (get-in state [:players "p1" :hand]))]
                  (-> (remove-card state "p1" card)
                      (get-in [:players "p1" :hand])
                      (first)
                      (:name)))
                "Defender")
           (is= (let [state (create-game [{:hand ["Imp" "Fireball" "War Golem"]}])
                      card (first (get-in state [:players "p1" :hand]))]
                  (-> (remove-card state "p1" card)
                      (get-in [:players "p1" :hand])
                      (first)
                      (:name)))
                "Fireball"))}
  [state player card]
  (assoc-in state [:players player :hand] (filter (fn[x] (not= (:id card) (:id  x))) (get-in state [:players player :hand]))))



(defn use-hero-power
  "Use hero power. Call the hero power function and reduce mana"
  {:test (fn []
           (is= (-> (use-hero-power (init-game ["Jaina Proudmoore","Anduin Wrynn"]) "p1" "h2")
                    (get-in [:players "p1" :mana]))
                -1))}
  [state player-id target-id ]
  (let [hero-power (hero-power(get-in state [:players player-id :hero :name]))]
    (as-> (reduce-mana state player-id (:mana-cost hero-power)) $
          ((:power hero-power) $ player-id target-id))))


(defn play-spell-card
  "Play a spell card save the spell card in the state if it has a secret"
  {:test (fn []
           (is= (let [state (-> (create-game)
                                (assoc-in [:players "p1" :hand] (list {:name "Snake Trap", :entity-type :card, :id 1}
                                                                      {:name "Snake Trap", :entity-type :card, :id 2}
                                                                      {:name "Bananas", :entity-type :card, :id 3}))
                                (assoc-in [:players "p1" :secrets] (list))
                                (assoc-in [:players "p1" :mana] 10))
                      card (first (get-in state [:players "p1" :hand]))]
                  (-> (play-spell-card state "p1" card)
                      (get-in [:players "p1" :hand])
                      (count)))
                2)
           (is= (let [state (-> (create-game)
                                (assoc-in [:players "p1" :hand] (list {:name "Snake Trap", :entity-type :card, :id 1}
                                                                      {:name "Snake Trap", :entity-type :card, :id 2}
                                                                      {:name "Bananas", :entity-type :card, :id 3}))
                                (assoc-in [:players "p1" :secrets] (list))
                                (assoc-in [:players "p1" :mana] 10))
                      card1 {:name "Bananas", :entity-type :card, :id 3}
                      card2 {:name "Snake Trap", :entity-type :card, :id 2}
                      card3 {:name "Snake Trap", :entity-type :card, :id 1}]
                  (-> (play-spell-card state "p1" card1)
                      (play-spell-card "p1" card2)
                      (play-spell-card "p1" card3)
                      (get-in [:players "p1" :secrets])
                      (count)))
                2)
           (is= (let [state (-> (create-game)
                                (assoc-in [:players "p1" :hand] (list {:name "Snake Trap", :entity-type :card, :id 1}
                                                                      {:name "Snake Trap", :entity-type :card, :id 2}
                                                                      {:name "Bananas", :entity-type :card, :id 3}))
                                (assoc-in [:players "p1" :secrets] (list))
                                (assoc-in [:players "p1" :mana] 10))
                      card {:name "Snake Trap", :entity-type :card, :id 1}]
                  (-> (play-spell-card state "p1" card)
                      (get-in [:players "p1" :secrets])
                      (first)
                      (:name)))
                "Snake Trap")
           (is= (let [state (-> (create-game)
                                (assoc-in [:players "p1" :hand] (list {:name "Snake Trap", :entity-type :card, :id 1}
                                                                      {:name "Snake Trap", :entity-type :card, :id 2}
                                                                      {:name "Bananas", :entity-type :card, :id 3}))
                                (assoc-in [:players "p1" :secrets] (list)))
                      card {:name "Bananas", :entity-type :card, :id 3}]
                  (-> (play-spell-card state "p1" card)
                      (get-in [:players "p1" :secrets])
                      ))
                (list))
           (let [state (-> (create-game
                             [{:minions [(create-minion "Imp" :id 1)]}
                              {:minions [(create-minion "War Golem" :id 2)]}])
                           (play-spell-card "p1" {:name "Snake Trap"
                                                  :entity-type :card}))])
           (is= (let [state (-> (create-game)
                                (assoc-in [:players "p1" :hand] (list {:name "Snake Trap", :entity-type :card, :id 1}
                                                                      {:name "Snake Trap", :entity-type :card, :id 2}
                                                                      {:name "Bananas", :entity-type :card, :id 3}))
                                (assoc-in [:players "p1" :secrets] (list))
                                (assoc-in [:players "p1" :mana] 10))
                      card {:name "Snake Trap", :entity-type :card, :id 1}]
                  (-> (play-spell-card state "p1" card)
                      (get-in [:players "p1" :mana])))
                8))}
  [state player-id card]
    (let [card-def  (get-definition card)
          new-state (if (contains? card-def :secret)
                      (add-secret state {:player-id player-id :secret (create-secret (:name card-def))})
                      state)]
      (-> new-state
          (remove-card player-id card)
          (reduce-mana player-id (:mana-cost card-def)))))

(defn summon-minion
  {:test (fn []
           (is= (let [state (init-game ["Jaina Proudmoore","Anduin Wrynn"])]
                  (-> (summon-minion state "p1" "Snake" 0)
                      (get-in [:players "p1" :minions])
                      (count)
                      ))
                1)
           (is= (let [state (init-game ["Jaina Proudmoore","Anduin Wrynn"])]
                  (-> (summon-minion state "p1" "Snake" 0)
                      (get-in [:players "p1" :minions])
                      (first)
                      (:name)
                      ))
                "Snake"
                )
           (is= (let [state (init-game ["Jaina Proudmooore" "Rexxar"])]
                  (-> (summon-minion state "p1" "Snake" 0)
                      (get :minion-ids-summoned-this-turn)))
                ["m1"])
           (is= (let [state (create-game [{:minions ["Imp" "Imp" "Imp" "Imp" "Imp" "Imp" "Imp"]}])]
                  (-> (summon-minion state "p1" "Snake")
                      (get-in [:players "p1" :minions])
                      (count)))
                7)
           (is= (let [state (create-game [{:minions ["Imp" "Imp" "Imp" "Imp" "Imp" "Imp"]}])]
                  (-> (summon-minion state "p1" "Snake")
                      (get-in [:players "p1" :minions])
                      (count)))
                7))}
  ([state player name]
   (summon-minion state player name 0))

  ([state player name pos]
   (if (< (count(get-minions state player)) 7)
     (as-> (add-minion-to-board state {:player-id player :minion (create-minion name) :position pos}) $
           ;; Minion is "sleepy"
           (update-in $ [:minion-ids-summoned-this-turn] (fn [minions] (conj minions (:id (first (filter (fn [x] (= (:position x) pos)) (get-minions $ player))))))))
     state)))


(defn play-minion-card
  "Place the minion card with on the board with a given position"
  {:test (fn []
           (is= (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
                           card (first (get-in state [:players "p1" :hand]))]
                  (-> (play-minion-card state "p1" card 0)
                      (get-in [:players "p1" :hand])
                      (count)))
                2)
           (is= (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
                      card (first (get-in state [:players "p1" :hand]))]
                  (-> (play-minion-card state "p1" card 0)
                      (get-events)
                      (count)))
                1)
           (is= (let [state (create-game [{:hand ["Imp" "Defender" "War Golem"]}])
                      card (first (get-in state [:players "p1" :hand]))]
                  (-> (play-minion-card state "p1" card 0)
                      (get-events)
                      (first)
                      (:minion)
                      (:name)))
                "Imp")
           (is= (-> (create-game)
                    (play-minion-card "p1" {:name "Arcane Golem", :entity-type :card, :id 5} 0)
                    (get-events)
                    (first)
                    (get-in [:minion :attack]))
                4))}
  [state player-id card pos & [target-id args]]
  {:pre [(-> (get-definition (:name card))
             (:type)
             (= :minion))
         ;;Too many arguments:
         (nil? args)]}
  (let [card (merge (get-definition (:name card)) card)
        minion (create-minion (:name card))
        battlecry (fn [state] ((:battlecry card) state player-id (get-minion state target-id)))]
    (-> state
        (enqueue-event :summon-minion player-id minion pos)
        (remove-card player-id card)
        (reduce-mana player-id (:mana-cost card))
        (cond-> (contains? card :battlecry)
                (battlecry)))))

(defn end-turn
  {:test (fn []
           (let [state (create-game)]
             (is= (-> state
                      (end-turn "p1")
                      (end-turn "p2")
                      (:player-id-in-turn))
                  "p1")))}
  [state player-id]
  {:pre  [(or (nil? player-id)
              (= (:player-id-in-turn state)
                 player-id))]
   :post [(fn [ret] (not (nil? ret)))]}
  (let [next-player (get-opponent-id state (:player-id-in-turn state))
        own-minions (get-minions state (:player-id-in-turn state))]
    (as-> (assoc state :player-id-in-turn next-player) $
          (assoc $ :minion-ids-summoned-this-turn [])
          ;; TODO (Robert): Should this be done in a start-of-turn function?
          (enqueue-event $ :draw-card (:player-id-in-turn $))
          (increase-turn-counter $)
          (reduce (fn [s m]
                    (if (and (frozen? s (:id m))
                             (< (attacks-performed-this-turn s (:id m)) 1))
                      (remove-character-state s (:id m) "FROZEN")
                      s))
                  $ own-minions)
          (remove-character-state $ (:player-id-in-turn state) "FROZEN")
          (dissoc $ :turn)
          (enqueue-event $ :start-of-turn next-player))))

(defn start-turn
  {:test (fn []
           (let [state (create-game)]
             (is= (-> state
                      (assoc :player-id-in-turn "p2")
                      (assoc-in [:players "p2" :max-mana] 7)
                      (start-turn "p2")
                      (get-mana "p2"))
                  8)
             (is= (-> state
                      (assoc :player-id-in-turn "p2")
                      (start-turn "p2")
                      (get-mana "p1"))
                  1)
             (is= (-> state
                      (assoc-in [:players "p1" :max-mana] 7)
                      (start-turn "p1")
                      (get-mana "p1"))
                  8)
             (is= (-> state
                      (start-turn "p1")
                      (get-mana "p2"))
                  1)))}
[state player-id]
  {:pre  [(or (nil? player-id)
              (= (:player-id-in-turn state)
                 player-id))]}
  (let [player-id (:player-id-in-turn state)
        new-state (add-max-mana state 1 player-id)
        new-max-mana (get-max-mana new-state player-id)]
    (-> new-state
          (add-mana new-max-mana player-id))))

(defn shuffle-deck
  "Shuffle a players deck and update the seed"
  {:test (fn []
           (is= (-> (create-game)
                    (set-seed 2)
                    (assoc-in [:players "p1" :deck] (list {:name "Acolyte of Pain", :entity-type :card, :id 7}
                                                          {:name "War Golem", :entity-type :card, :id 9}))
                    (assoc-in [:players "p2" :deck] (list {:name "War Golem", :entity-type :card, :id 10}
                                                          {:name "Snake", :entity-type :card, :id 12}))
                    (shuffle-deck "p1")
                    (get-seed))
                2260595942425364)
           (is= (-> (init-game ["Jaina Proudmoore" "Rexxar"])
                    (set-seed 1)
                    (assoc-in [:players "p1" :deck] (list {:name "Acolyte of Pain", :entity-type :card, :id 7}
                                                          {:name "War Golem", :entity-type :card, :id 9}))
                    (assoc-in [:players "p2" :deck] (list {:name "War Golem", :entity-type :card, :id 10}
                                                          {:name "Snake", :entity-type :card, :id 12}))
                    (shuffle-deck "p1")
                    (shuffle-deck "p2")
                    (get-seed))
                -9136436700791295257)
           (is= (as-> (init-game ["Jaina Proudmoore" "Rexxar"]) $
                      (set-seed $ 1)
                      (assoc-in $ [:players "p1" :deck] (list {:name "Acolyte of Pain", :entity-type :card, :id 7}
                                                              {:name "War Golem", :entity-type :card, :id 9}))
                      (assoc-in $ [:players "p2" :deck] (list {:name "War Golem", :entity-type :card, :id 10}
                                                            {:name "Snake", :entity-type :card, :id 12}))
                      (shuffle-deck $ "p1")
                      (get-deck $ "p1")
                      (map (fn [x] (x :id)) $))
                (list 9 7))
           (is= (as-> (init-game ["Jaina Proudmoore" "Rexxar"]) $
                      (set-seed $ 1)
                      (assoc-in $ [:players "p1" :deck] (list {:name "Acolyte of Pain", :entity-type :card, :id 7}
                                                              {:name "War Golem", :entity-type :card, :id 9}))
                      (assoc-in $ [:players "p2" :deck] (list {:name "War Golem", :entity-type :card, :id 10}
                                                            {:name "Snake", :entity-type :card, :id 12}))
                      (shuffle-deck $ "p2")
                      (shuffle-deck $ "p2")
                      (get-deck $ "p2")
                      (map (fn [x] (x :id)) $))
                (list 10 12)))
   }
  [state player-id]
  (let [new-deck  (shuffle-with-seed (get-seed state) (get-deck state player-id))]
    (-> (set-seed state (first new-deck))
        (assoc-in [:players player-id :deck] (second new-deck)))))