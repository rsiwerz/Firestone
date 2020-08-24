(ns firestone.events.battlecries
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.error :refer [error]]
            [firestone.construct :refer [add-minion-to-board
                                         create-card
                                         create-game
                                         create-minion
                                         create-secret
                                         generate-id
                                         get-minions
                                         get-minion
                                         get-players
                                         get-secrets
                                         remove-minion
                                         get-opponent-id]]
            [firestone.definitions :refer [get-definition]]
            [firestone.core :refer [add-max-mana
                                    get-max-mana
                                    get-mana
                                    get-hand]]
            [firestone.definition.minion-modifiers :refer [decrease-attack
                                                           increase-attack
                                                           increase-attack-this-turn
                                                           decrease-attack-this-turn
                                                           increase-health]]
            [firestone.util :refer [flip-partial
                                    trace]]
            [firestone.events.queue :refer [enqueue-event
                                            get-events
                                            qflatten]]))

(def eater-name "Eater of Secrets")

(defn- buff-eater
  {:test (fn []
           (is= (buff-eater 3 {:event-type :end-turn})
                {:event-type :end-turn})
           (is= (-> (buff-eater 3 {:event-type  :summon-minion
                                   :minion-name eater-name
                                   :player-id   "p1"})
                    (:minion)
                    (select-keys [:name :card-type :attack :health]))
                (-> (get-definition eater-name)
                    (assoc :attack 5
                           :health 7)
                    (select-keys [:name :card-type :attack :health])))
           (is= (-> (create-game [{:minions ["Eater of Secrets"]}])
                    (get-minion "m1")
                    (select-keys [:name :card-type :attack :health])
                    ;;It's ok, it's just data
                    (->> (assoc {:event-type :summon-minion
                                 :player-id  "p2"
                                 :position   1}
                           :minion)
                         (buff-eater 3)
                         (:minion)))
                (-> (get-definition eater-name)
                    (assoc :attack 5
                           :health 7)
                    (select-keys [:name :card-type :attack :health]))))}
  [buff event]
  (let [attack (+ buff (:attack (create-minion eater-name)))
        health (+ buff (:health (create-minion eater-name)))]
    (if-not (= (:event-type event)
               :summon-minion)
      event
      (cond-> event
              (= (:minion-name event)
                 eater-name)
              (-> (dissoc :minion-name)
                  (assoc :minion (create-minion eater-name :attack attack
                                                :health health)))
              (= (:name (:minion event))
                 eater-name)
              (-> (update-in [:minion :attack] (partial + buff))
                  (update-in [:minion :health] (partial + buff)))))))

(defn eater-of-secrets
  "Battlecry: Destroy all enemy Secrets. Gain +1/+1 for each."
  [state owner-id {}]
  {:pre [(some? owner-id)
         (not (map? owner-id))]}
  (let [
        enemy (->> (get-players state)
                   (filter (fn [{enemy-id :id}]
                             (not= owner-id enemy-id)))
                   (first))
        enemy-secrets (:secrets enemy)
        buff (count enemy-secrets)]
    (-> state
        (update-in [:players (:id enemy)] dissoc :secrets)
        ;; Make sure :events is a list
        (update :events qflatten)
        ;; It's ok it's just data
        (update :events (partial map (partial buff-eater buff))))))


(defn big-game-hunter-targets
  "Find the valid targets for the Big Game Hunter battlecry"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "War Golem"]}])]
             (is= (->> state
                       (big-game-hunter-targets))
                  ["m2" "m3"])))}
  [state]
  (let [minions (get-minions state)]
    (->> minions (filter (fn [minion] (>= (:attack minion) 7)))
         (map :id))))

(defn big-game-hunter
  "Battlecry: Destroy a minion with an Attack of 7 or more."
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "War Golem" :id 2)
                                                (create-minion "Imp")]}]
                                    :player-id-in-turn "p2")]
           ;;TODO: Should have a proper test.
             ))}
  [state owner {minion-id :id}]
  (if minion-id
    (enqueue-event state {:event-type :destroy-minion
                          :player-id owner
                          :minion-id minion-id})
    state))

(defn arcane-golem
  "Battlecry: Give your opponent a Mana Crystal."
  {:test (fn []
           (let [state (create-game)]
             (is= (-> state
                      (add-max-mana 3 "p2")
                      (arcane-golem "p1" "m1")
                      (get-max-mana "p2"))
                  5)))}
  [state owner {minion-id :id}]
  (let [target-player-id (get-opponent-id state owner)]
    (add-max-mana state 1 target-player-id)))

(defn king-mukla
  "Battlecry: Give your opponent 2 Bananas."
  {:test (fn []
           (let [state (create-game)]
             (is= (as-> state $
                    (king-mukla $ "p2" "m1")
                    (get-hand $ "p1")
                    (filter (fn [card] (= "Bananas" (:name card))) $)
                    (count $)
                    )
                  2)))}
  [state owner {minion-id :id}]
  (let [target-player (->> state
                           (get-players)
                           (filter (fn [player]
                                     (not= owner player)))
                           (first)
                           (:id))
        [{first-id :counter} second-id] (generate-id state)]
    (assoc-in state [:players target-player :hand] [(create-card "Bananas" :id (str "b" first-id))
                                                    (create-card "Bananas" :id (str "b" second-id))])))

(defn valid-abusive-sergeant-targets
  "Get the minions as valid targets"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]
                                      :hand ["Abusive Sergeant"]}])]
             (is= (-> state
                      valid-abusive-sergeant-targets)
                  ["m1" "m2" "m3"])))}
  [state]
  (map :id (get-minions state)))

(defn abusive-sergeant
  "Battlecry: Give a minion +2 Attack this turn."
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "Imp" :id "i1")]}])]
             (is= (-> state
                      (abusive-sergeant "p2" (get-minion state "i1"))
                      (get-minion "i1")
                      (:attack))
                  (-> (get-definition "Imp")
                      (:base-attack)
                      (+ 2)))))}
  [state owner {minion-id :id}]
  (if minion-id
    (let [attack 2]
      (increase-attack-this-turn state attack (get-minion state minion-id)))
    state))

(defn valid-shrinkmeister-targets
  "Get the minions as valid targets"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem" "Arcane Golem"]
                                      :hand ["Shrinkmeister"]}])]
             (is= (-> state
                      (valid-shrinkmeister-targets))
                  ["m1" "m2" "m3"])))}
  [state]
  (map :id (get-minions state)))

(defn shrinkmeister
  "Battlecry: Give a minion -2 Attack this turn."
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "War Golem" :id "w1")]}])]
             (is= (-> (shrinkmeister state "p2" (get-minion state "w1"))
                      (get-minion "w1")
                      (:attack))
                  (-> (get-definition "War Golem")
                      (:base-attack)
                      (- 2)))))
   }
  [state owner minion]
  (if minion
    (let [attack 2]
      (decrease-attack-this-turn state attack minion))
    state))

(defn cabal-shadow-priest-targets
  "Find the valid targets for the Cabal Shadow Priest battlecry"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "Imp" "War Golem"]}])]
             (is= (->> (cabal-shadow-priest-targets state)
                       (map :id))
                  ["m1" "m2"])))}
  [state]
  (let [minions (get-minions state)]
    (filter (fn [minion] (<=(:attack minion) 2)) minions)))


(defn cabal-shadow-priest
  "Battlecry: Take control of an enemy minion that has 2 or less Attack."
  {:test (fn []
           (let [state (create-game [{:hand ["Cabal Shadow Priest"]}
                                     {:minions ["Imp"]}])
                 minion-id (-> (get-minions state)
                               (first)
                               (:id))]
             (is= (-> state
                      (cabal-shadow-priest "p1" (get-minion state minion-id))
                      (get-minion minion-id)
                      (:owner-id))
                  "p1")))}
  [state owner {minion-id :id}]
  (if minion-id
    (let [minion (get-minion state minion-id)]
      (-> state
          (remove-minion minion-id)
          (add-minion-to-board {:player-id owner :minion minion :position 0})))
    state))

(defn valid-the-black-knight-targets
  "Find the valid targets for the Cabal Shadow Priest battlecry"
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "Imp" "Shieldbearer"]}])]
             (is= (->> (valid-the-black-knight-targets state))
                  ["m3"])))}
  [state]
  (let [minions (get-minions state)]
    (->> (filter (fn [minion] (= (:taunt (get-definition minion)) true)) minions)
         (map :id))))

(defn the-black-knight
  "Battlecry: Destroy an enemy minion with Taunt"
  {:test (fn []
           (let [state (create-game [{:minions ["Shieldbearer"]}])
                 minion-id (-> (get-minions state)
                               (first)
                               (:id))]
             (is= (-> state
                      (the-black-knight "p1" (get-minion state minion-id))
                      (get-events))
                  [{:event-type :destroy-minion
                    :player-id "p1"
                    :minion-id minion-id}])))}
  [state owner {minion-id :id}]
  (if minion-id
    (enqueue-event state {:event-type :destroy-minion
                          :player-id owner
                          :minion-id minion-id})
    state))
