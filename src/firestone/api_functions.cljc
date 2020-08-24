(ns firestone.api-functions
  (:require [clojure.test :refer [function?]]
            [ysera.test :refer [is=]]
            [firestone.actions :as actions :refer [init-game]]
            [firestone.events.stepper :refer [trace-events
                                              step-events]]
            [firestone.construct :refer [create-minion
                                         create-game
                                         create-secret
                                         create-hero
                                         get-minion
                                         get-minions
                                         get-heroes
                                         set-seed
                                         get-seed
                                         get-opponent-id
                                         update-minion]]
            [firestone.core :refer [get-character
                                    valid-attack?
                                    player-attribute
                                    enough-mana?]]
            [firestone.events.queue :refer [enqueue-event]]
            [firestone.definitions :refer [get-definition]]))


(defn get-player-id-in-turn
  "Return the player in turn"
  {:test (fn []
           (let [state (init-game ["Jaina Proudmoore" "Rexxar"])]
             (is= (get-player-id-in-turn state)
                  "p1")
             (is= (-> state
                      (actions/end-turn "p1")
                      (get-player-id-in-turn))
                  "p2")))}
  [state]
  (:player-id-in-turn state))

(defn get-valid-attack-ids
  {:test (fn []
           (let [state (-> (create-game [{:hero (create-hero "Anduin Wrynn")}])
                           (actions/play-minion-card "p1" {:name "Arcane Golem", :entity-type :card} 0)
                           (actions/play-minion-card "p1" {:name "Arcane Golem", :entity-type :card} 1)
                           (actions/play-minion-card "p2" {:name "Arcane Golem", :entity-type :card} 2)
                           (step-events)
                           (update-minion "m1" :cant-attack false)
                           (assoc-in [:players "p1" :hero :owner-id] "p1")
                           (assoc-in [:players "p2" :hero :owner-id] "p2")
                           (assoc-in [:minion-ids-summoned-this-turn] []))]
             (is= (get-valid-attack-ids state "m1")
                  ["h2" "m3"])))}

  [state attacker-id]
  (let [all-minions (get-minions state)
        all-heroes (get-heroes state)]
    (->> (concat all-heroes all-minions)
         (map :id)
         (filter (fn [target-id]
                   (valid-attack? state attacker-id target-id))))))

(defn get-character-states
  "Returning the states that a character can be in, frozen, stealth etc."
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "Blood Imp" :id "bi")]}])
                 minion (get-minion state "bi")]
             (is= (get-character-states minion)
                  ["STEALTH"])))}
  [character]
  (get-in character [:char-states]))

(defn new-game
  [game-id]

  (-> (init-game ["Anduin Wrynn" "Jaina Proudmoore"])
      (assoc :game-id game-id)
      (enqueue-event :start-of-turn "p1")
      (trace-events)))

(defn play-minion-card
  [state game-id player-id card-id position target-id]

  (let [card (->> (player-attribute state :hand player-id)
                  (filter (fn [card] (= (:id card) card-id)))
                  (first))
        card-def (get-definition (:name card))]
    (-> (enqueue-event state :play-card player-id (merge card-def card) position target-id)
        (trace-events))))

(defn play-spell-card
  [state game-id player-id card-id target-id]
  (let [card (->> (player-attribute state :hand player-id)
                  (filter (fn [card] (= (:id card) card-id)))
                  (first))
        card-def (get-definition (:name card))
        state (if (contains? card-def :spells)
                ((:spells card-def) state player-id target-id)
                state)]

      (-> (enqueue-event state :play-card player-id (merge card-def card))
          (trace-events))))

(defn attack
  [state game-id player-id attacker-id target-id]
  (if (-> (get-character state target-id)
          :entity-type
          (= :hero))
    (-> (enqueue-event state {:event-type :attack-hero
                              :attacker-id    attacker-id
                              :target-id      target-id})
        (trace-events))
    (-> (enqueue-event state {:event-type :attack-minion
                              :attacker-id    attacker-id
                              :target-id      target-id})
        (trace-events))))

(defn end-turn
  [state player-id]
  (-> (enqueue-event state :end-turn)
      (trace-events)))

(defn use-hero-power
  [state game-id player-id target-id]
  (-> (enqueue-event state {:event-type :use-hero-power
                            :player-id  player-id
                            :target-id  target-id})
      (trace-events)))

(defn get-player-ids
  {:test (fn []
           (is= (-> (init-game ["Jaina Proudmoore" "Rexxar"])
                    (get-player-ids))
                (list "p1" "p2")))}
  [state]
  (-> state
      (get :players)
      (keys)))