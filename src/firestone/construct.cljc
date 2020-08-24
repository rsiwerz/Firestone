(ns firestone.construct
  (:require [clojure.test :refer [function?]]
            [ysera.test :refer [is is-not is= error?]]
            [ysera.collections :refer [seq-contains?]]
            [firestone.definitions :refer [get-definition]]))

(declare create-game)

(defn generate-id
  "Generates an id and returns a tuple with the new state and the generated id."
  {:test (fn []
           (is= (generate-id {:counter 6})
                [{:counter 7} 6]))}
  [state]
  [(update state :counter inc) (:counter state)])

(defn generate-time-id
  "Generates an number and returns a tuple with the new state and the generated number."
  {:test (fn []
           (is= (generate-id {:counter 6})
                [{:counter 7} 6]))}
  [state]
  [(update state :counter inc) (:counter state)])

(defn create-hero
  "Creates a hero from its definition by the given hero name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-hero "Jaina Proudmoore")
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 0})
           (is= (create-hero "Jaina Proudmoore" :damage-taken 10)
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 10}))}
  [name & kvs]
  (let [hero {:name         name
              :entity-type  :hero
              :damage-taken 0}]
    (if (empty? kvs)
      hero
      (apply assoc hero kvs))))

(defn create-card
  "Creates a card from its definition by the given card name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-card "Imp" :id "i")
                {:id          "i"
                 :entity-type :card
                 :card-type   :minion
                 :name        "Imp"}))}
  [name & kvs]
  (let [card-def (get-definition name)
        card {:name        name
              :entity-type :card}]
    (cond-> (if (empty? kvs)
              card
              (apply assoc card kvs))
            (:type card-def) (assoc :card-type (:type card-def))
            (:spell card-def) (assoc :card-type :spell))))

(defn create-minion
  "Creates a minion from its definition by the given minion name. The additional key-values will override the default values."
  {:test (fn []
           (is= (create-minion "Imp" :id "i" :damage-taken 1)
                {:damage-taken                1
                 :health                      1
                 :attack                      1
                 :entity-type                 :minion
                 :name                        "Imp"
                 :id                          "i"})
           (is= (create-minion (create-minion "Snake"))
                (create-minion "Snake")))}
  [name-or-entity & kvs]
  {:pre [(or (string? name-or-entity)
             (and (map? name-or-entity)
                  (contains? name-or-entity :name)
                  (string? (:name name-or-entity))))]
   :post [(comp string? :name)]}
  (let [definition (get-definition name-or-entity)
        name (if (string? name-or-entity)
               name-or-entity
               (:name name-or-entity))
        minion (cond-> {:damage-taken                0
                        :entity-type                 :minion
                        :name                        name
                        :health                      (:health definition)
                        :attack                      (:base-attack definition)}
                       (map? name-or-entity) (merge name-or-entity))]
    (if (empty? kvs)
      minion
      (apply assoc minion kvs))))

(defn create-secret
  {:test (fn []
           (is= (create-secret "Snake Trap"
                               :id "i"
                               :owner-id "p1")
                {:entity-type                 :secret
                 :name                        "Snake Trap"
                 :id                          "i"
                 :owner-id                    "p1"}))}
  [name & kvs]
  (let [secret {:entity-type  :secret
                :name         name}]
    (if (empty? kvs)
      secret
      (apply assoc secret kvs))))



(defn create-empty-state
  "Creates an empty state with the given heroes."
  {:test (fn []
           (is= (create-empty-state [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                (create-empty-state))

           (is= (create-empty-state [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                {:player-id-in-turn             "p1"
                 :players                       {"p1" {:id      "p1"
                                                       :mana    1
                                                       :max-mana 1
                                                       :deck    []
                                                       :hand    []
                                                       :minions []
                                                       :hero    {:name         "Jaina Proudmoore"
                                                                 :id           "h1"
                                                                 :damage-taken 0
                                                                 :entity-type  :hero}}
                                                 "p2" {:id       "p2"
                                                       :mana    1
                                                       :max-mana 1
                                                       :deck    []
                                                       :hand    []
                                                       :minions []
                                                       :hero     {:name         "Jaina Proudmoore"
                                                                 :id           "h2"
                                                                 :damage-taken 0
                                                                 :entity-type  :hero}}}
                 :counter                       1
                 :minion-ids-summoned-this-turn []}))}
  ([heroes]
    ; Creates Jaina Proudmoore heroes if heroes are missing.
   (let [heroes (->> (concat heroes [(create-hero "Jaina Proudmoore")
                                     (create-hero "Jaina Proudmoore")])
                     (take 2))]
     {:player-id-in-turn             "p1"
      :players                       (->> heroes
                                          (map-indexed (fn [index hero]
                                                         {:id       (str "p" (inc index))
                                                          :mana     1
                                                          :max-mana 1
                                                          :deck     []
                                                          :hand     []
                                                          :minions  []
                                                          :hero     (assoc hero :id (str "h" (inc index)))}))
                                          (reduce (fn [a v]
                                                    (assoc a (:id v) v))
                                                  {}))
      :counter                       1
      :minion-ids-summoned-this-turn []}))
  ([]
   (create-empty-state [])))

(defn get-player
  "Returns the player with the given id."
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-player "p1")
                    (:id))
                "p1"))}
  [state player-id]
  (get-in state [:players player-id]))


(defn- merge-declaration
  [entity]
  (merge (get-definition (:name entity))
         entity))

(defn- merge-turnbuff
  [state minion]
  (merge-with (fn [x y] (max 0 (+ x y)))
              minion
              (get-in state [:turn :buffs (:id minion)])))

(declare get-players)

(defn get-minions
  "Returns the minions on the board for the given player-id or for both players.
   The map returned is a union of the map in the player, temporary buffs and the definition."
  {:test (fn []
           (is= (-> (create-empty-state)
                    (get-minions "p1"))
                [])
           (is= (-> (create-empty-state)
                    (get-minions))
                []))}
  ([state player-id]
   {:post [(some? %)
           (every? #(some? (:owner-id %)) %)]}
   (some->> (:minions (get-player state player-id))
            (map merge-declaration)
            (map (partial merge-turnbuff state))
            (map #(assoc % :owner-id player-id))))
  ([state]
   (some->> (get-players state)
            (map :id)
            (map (partial get-minions state))
            (flatten))))

(defn add-minion-to-board
  "Adds a minion with a given position to a player's minions and updates the other minions' positions."
  {:test (fn []
           ; Adding a minion to an empty board
           (is= (as-> (create-empty-state) $
                      (add-minion-to-board $ {:player-id "p1" :minion (create-minion "Imp" :id "i") :position 0})
                      (get-minions $ "p1")
                      (map (fn [m] {:id (:id m) :name (:name m)}) $))
                [{:id "i" :name "Imp"}])
           ; Adding a minion and update positions
           (let [minions (-> (create-empty-state)
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i1") :position 0})
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i2") :position 0})
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i3") :position 1})
                             (get-minions "p1"))]
             (is= (map :id minions) ["i1" "i2" "i3"])
             (is= (map :position minions) [2 0 1]))
           ; Generating an id for the new minion
           (let [state (-> (create-empty-state)
                           (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp") :position 0}))]
             (is= (-> (get-minions state "p1")
                      (first)
                      (:id))
                  "m1")
             (is= (:counter state) 2)))}
  [state {player-id :player-id minion :minion position :position}]
  {:pre [(map? state) (string? player-id) (map? minion) (number? position)]}
  (let [[state id] (if (contains? minion :id)
                     [state (:id minion)]
                     (let [[state value] (generate-id state)]
                       [state (str "m" value)]))]
    (update-in state
               [:players player-id :minions]
               (fn [minions]
                 (conj (->> minions
                            (mapv (fn [m]
                                    (if (< (:position m) position)
                                      m
                                      (update m :position inc)))))
                       (assoc minion :position position
                              :owner-id player-id
                              :id id))))))

(defn add-minions-to-board
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-minions-to-board $ "p1" [(create-minion "Imp")
                                                    (create-minion "War Golem")
                                                    (create-minion "Dalaran Mage")])
                      (get-minions $ "p1")
                      (map :name $))
                ["Imp" "War Golem" "Dalaran Mage"]))}
  [state player-id minions]
  (->> minions
       (map-indexed (fn [index minion] [index minion]))
       (reduce (fn [state [index minion]]
                 (add-minion-to-board state {:player-id player-id
                                             :minion    (if (string? minion)
                                                          (create-minion minion)
                                                          minion)
                                             :position  index}))
               state)))

(defn- add-card-to
  "Adds a card to either the hand or the deck."
  [state player-id card-or-name place]
  (let [card (if (string? card-or-name)
               (create-card card-or-name)
               card-or-name)
        [state id] (if (contains? card :id)
                     [state (:id card)]
                     (let [[state value] (generate-id state)]
                       [state (str "c" value)]))
        ready-card (assoc card :owner-id player-id
                               :id id)]
    (update-in state [:players player-id place] conj ready-card)))

(defn add-card-to-deck
  [state player-id card]
  (add-card-to state player-id card :deck))

(defn add-card-to-hand
  [state player-id card]
  (add-card-to state player-id card :hand))

(defn add-cards-to-deck
  [state player-id cards]
  (reduce (fn [state card]
            (add-card-to-deck state player-id card))
          state
          cards))

(defn add-cards-to-hand
  [state player-id cards]
  (reduce (fn [state card]
            (add-card-to-hand state player-id card))
          state
          cards))

(defn get-players
  {:test (fn []
           (is= (->> (create-game)
                     (get-players)
                     (map :id))
                ["p1" "p2"]))}
  [state]
  {:pre  [(some? state)]
   :post [(= (count %) 2)]}
  (->> (:players state)
       (vals)))

(defn player-id?
  [state id]
  (seq-contains? (->> (get-players state)
                      (map :id))
                 id))

(defn get-minion
  "Returns the minion with the given id.
  The map returned is a union of the map in the player, temporary buffs and the definition."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-minion "i")
                    (:name))
                "Imp")
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-minion "i"))
                (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (get-in [:players "p1" :minions])
                    (first)
                    (merge (get-definition "Imp"))))
           (is= (-> (create-game [{:minions [(create-minion "Acolyte of Pain" :id "i")]}])
                    (get-minion "i"))
                (-> (create-game [{:minions [(create-minion "Acolyte of Pain" :id "i")]}])
                    (get-in [:players "p1" :minions])
                    (first)
                    (merge (get-definition "Acolyte of Pain")))))}
  [state id]
  (->> (get-minions state)
       (filter (fn [m] (= (:id m) id)))
       (first)))


(defn get-secrets
  {:test (fn []
           (is= (->> {:player-id-in-turn             "p1",
                     :players                       {"p1" {:id      "p1",
                                                           :deck    [],
                                                           :hand    [],
                                                           :minions [{:damage-taken                0,
                                                                      :entity-type                 :minion,
                                                                      :name                        "Imp",
                                                                      :attack                      1,
                                                                      :id                          1,
                                                                      :position                    0,
                                                                      :owner-id                    "p1"}],
                                                           :hero    {:name         "Jaina Proudmoore",
                                                                     :entity-type  :hero,
                                                                     :damage-taken 0,
                                                                     :id           "h1"}},
                                                     "p2" {:id      "p2",
                                                           :deck    [],
                                                           :hand    [],
                                                           :minions [{:damage-taken                0,
                                                                      :entity-type                 :minion,
                                                                      :name                        "War Golem"
                                                                      :attack                      7,
                                                                      :id                          2,
                                                                      :position                    0,
                                                                      :owner-id                    "p2"}],
                                                           :hero    {:name         "Jaina Proudmoore",
                                                                     :entity-type  :hero,
                                                                     :damage-taken 0, :id "h2"},
                                                           :secrets [{:entity-type :secret,
                                                                      :name        "Snake Trap",
                                                                      :id          "c3",
                                                                      :owner-id    "p2"}]}},
                     :counter                       1,
                     :minion-ids-summoned-this-turn [],
                     :events                        [{:event-type :attack-minion, :attacker-id 1, :target-id 2}]}
                    (get-secrets)
                    (map (fn [x] (dissoc x :trigger))))
                [{:name        "Snake Trap"
                  :id          "c3"
                  :owner-id    "p2"
                  :entity-type :secret
                  :mana-cost   2
                  :type        :spell
                  :set         :classic
                  :class       :hunter
                  :rarity      :epic
                  :description "Secret: When one of your minions is attacked summon three 1/1 Snakes."
                  :secret      true}])
           )}
  ([state]
   (->> state
        (:players)
        (vals)
        (map :secrets)
        (flatten)
        (filter (fn [{type :entity-type}]
                  (= type :secret)))
        (map merge-declaration)))
  ([state player-id]
   (->> (:secrets (get-player state player-id))
        (map (partial merge-turnbuff state))
        (map merge-declaration))))

(defn get-secret
  "Returns the a union of the definition and the board information
   of the secret with the given id."
  {:test (fn []
           (is= (let
                  [p1     {:id      "p1",
                           :deck    {:name "Snake Trap", :entity-type :card, :id 45},
                           :hand    (list {:name "Snake Trap", :entity-type :card, :id 1}),
                           :minions [],
                           :hero    {:name "Jaina Proudmoore", :entity-type :hero, :damage-taken 0, :id "h1"},
                           :secrets (list {:entity-type :secret, :name "Snake Trap", :id 2, :owner-id "p1"})}
                   p2     {:id      "p2"
                           :deck    [],
                           :hand    [],
                           :minions [],
                           :hero    {:name "Jaina Proudmoore", :entity-type :hero, :damage-taken 0, :id "h2"}}
                   state  {:player-id-in-turn  "p1"
                           :players            {"p1" p1
                                                "p2" p2}
                           :counter             1,
                           :minion-ids-summoned-this-turn []}]

                  (get-secret state 2))
                (merge (get-definition "Snake Trap")
                       {:entity-type :secret
                        :name        "Snake Trap"
                        :id          2
                        :owner-id    "p1"})))}
  [state id]
  (->> (get-secrets state)
       (filter
        (fn [{i :id}]
            (= id i)))
       (first)))



(defn add-secret
  {:test (fn []
           ; Adding a secret
           (is= (as-> (create-empty-state) $
                      (add-secret $ {:player-id "p1" :secret (create-secret "Snake Trap" :id "i")})
                      (get-secrets $ "p1")
                      (map (fn [m] {:id (:id m) :name (:name m)}) $))
                [{:id "i" :name "Snake Trap"}])
           ; Generating an id for the new secret
           (let [state (-> (create-empty-state)
                           (add-secret {:player-id "p1" :secret (create-secret "Snake Trap")}))]
             (is= (-> (get-secrets state "p1")
                      (first)
                      (:id))
                  "s1")
             (is= (:counter state) 2)))}
  [state {player-id :player-id secret :secret}]
  {:pre [(map? state) (string? player-id) (map? secret)]}
  (let [[state id] (if (contains? secret :id)
                     [state (:id secret)]
                     (let [[state value] (generate-id state)]
                       [state (str "s" value)]))]
    (update-in state
               [:players player-id :secrets]
               (fn [secrets]
                 (conj secrets
                       (assoc secret :owner-id player-id
                                     :id id))))))

(defn add-secrets
  {:test (fn []
           (is= (as-> (create-empty-state) $
                      (add-secrets $ "p1" [(create-secret "Snake Trap")
                                           (create-secret "Competitive Spirit")])
                      (get-secrets $ "p1")
                      (map :name $))
                ["Competitive Spirit" "Snake Trap"]))}
  [state player-id secrets]
  (->> secrets
       (reduce (fn [state secret]
                 (add-secret state {:player-id player-id
                                    :secret    (if (string? secret)
                                                 (create-secret secret)
                                                 secret)}))
               state)))


(defn get-hero
  [state player-id]
  {:pre (string? player-id)}
  (some-> (get-in state [:players player-id :hero])
          (assoc :owner-id player-id)))

(defn remove-secret
  "Removes a secret with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:secrets [(create-secret "Snake Trap" :id "s")]}])
                    (remove-secret "s")
                    (get-secrets))
                [])
           (is= (as-> (create-game [{:secrets [(create-secret "Snake Trap" :id "s1")
                                               (create-secret "Competitive Spirit" :id "s2")]}]) $
                      (remove-secret $ "s1")
                      (get-secrets $)
                      (map :name $))
                ["Competitive Spirit"]))}
  [state id]
  (let [owner-id (:owner-id (get-secret state id))]
    (update-in state
               [:players owner-id :secrets]
               (fn [secrets]
                 (remove (fn [s] (= (:id s) id)) secrets)))))

(defn get-heroes
  {:test (fn []
           (is= (->> (create-game)
                     (get-heroes)
                     (map :name))
                ["Jaina Proudmoore" "Jaina Proudmoore"])
           (is= (->> (create-game)
                     (get-heroes)
                     (first))
                {:name         "Jaina Proudmoore"
                 :entity-type  :hero
                 :damage-taken 0
                 :owner-id     "p1"
                 :id           "h1"}))}
  [state]
  (->> (:players state)
       (keys)
       (map (partial get-hero state))))

(defn get-seed
  {:test (fn []
           (is= (-> (create-game)
                    (get-seed))
                1))}
  [state]
  (if (nil? (get-in state [:seed]))
    1
    (get-in state [:seed])))


(defn set-seed
  {:test (fn []
           (is= (-> (create-game)
                    (set-seed 122)
                    (get-seed))
                122)
           (is= (-> (create-game)
                    (set-seed 12)
                    (get-seed))
                12))
   }
  [state seed]
  (assoc-in state [:seed] seed))

(defn replace-minion
  "Replaces a minion with the same id as the given new-minion."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "minion")]}])
                    (replace-minion (create-minion "War Golem" :id "minion"))
                    (get-minion "minion")
                    (:name))
                "War Golem"))}
  [state new-minion]
  (let [owner-id (or (:owner-id new-minion)
                     (:owner-id (get-minion state (:id new-minion))))]
    (update-in state
               [:players owner-id :minions]
               (fn [minions]
                 (map (fn [m]
                        (if (= (:id m) (:id new-minion))
                          new-minion
                          m))
                      minions)))))

(defn update-minion
  "Updates the value of the given key for the minion with the given id. If function-or-value is a value it will be the
   new value, else if it is a function it will be applied on the existing value to produce the new value."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (update-minion "i" :damage-taken inc)
                    (get-minion "i")
                    (:damage-taken))
                1)
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (update-minion "i" :damage-taken 2)
                    (get-minion "i")
                    (:damage-taken))
                2)
           ;; All other keys must be unchanged
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (update-minion "i" :id identity))
                (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}]))))}
  [state id key function-or-value]
  {:pre [(some? function-or-value)]}
  (let [minion (->> (get-in state [:players (:owner-id (get-minion state id)) :minions])
                    (filter #(= id (:id %)))
                    (first))]
    (replace-minion state (if (function? function-or-value)
                            (if (contains? minion key)
                              (update minion key function-or-value)
                              (assoc minion key (-> (merge-declaration minion)
                                                    (key)
                                                    (function-or-value))))
                            (assoc minion key function-or-value)))))


(defn update-hero
  "Works for both hero-id and player-id"
  {:test (fn []
           (is= (-> (create-game)
                    (update-hero "p1" :damage-taken inc)
                    (get-in [:players "p1" :hero :damage-taken]))
                1)
           (is= (-> (create-game)
                    (update-hero "p1" :damage-taken inc)
                    (get-in [:players "p2" :hero :damage-taken]))
                0)
           (is= (-> (create-game)
                    (update-hero "h1" :damage-taken inc)
                    (get-in [:players "p1" :hero :damage-taken]))
                1)
           (is= (-> (create-game)
                    (update-hero "h1" :damage-taken inc)
                    (get-in [:players "p2" :hero :damage-taken]))
                0)
           (is= (-> (create-game)
                    (update-hero "p1" :damage-taken 2)
                    (get-in [:players "p1" :hero :damage-taken]))
                2)
           (is= (-> (create-game)
                    (update-hero "p1" :damage-taken 2)
                    (get-in [:players "p2" :hero :damage-taken]))
                0)
           (is= (-> (create-game)
                    (update-hero "h2" :damage-taken inc)
                    (get-in [:players "p2" :hero :damage-taken]))
                1)
           (is= (-> (create-game)
                    (update-hero "p2" :damage-taken 2)
                    (get-in [:players "p2" :hero :damage-taken]))
                2))}
  [state id key function-or-value]
  {:pre [(some? function-or-value)]}
  (let [player-id (->> (get-players (create-game))
                       (map (fn [p] [(:id p) (get-in p [:hero :id])]))
                       (filter (fn [x]
                                 (or
                                   (= id (first x))
                                   (= id (second x)))))
                       (first)
                       (first))]
    (if (function? function-or-value)
      (update-in state [:players player-id :hero key] function-or-value)
      (assoc-in state [:players player-id :hero key] function-or-value))))


(defn get-dead-minions
  "Get all dead minions from the state"
  [state]
  (get state :graveyard))

(defn get-dead-minion
  "Get the dead minion from the graveyard with the given id"
  {:test (fn []
           (let [state (assoc (create-game) :graveyard [(create-minion "Imp" :id "m1")])]
             (is= (-> state
                      (get-dead-minion "m1")
                      (:name))
                  "Imp")))}
  [state id]
  (-> (filter (fn [m]
                (= (:id m) id))
              (get state :graveyard))
      (first)))

(defn remove-minion
  "Removes a minion with the given id from the state."
  {:test (fn []
           (is= (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                    (remove-minion "i")
                    (get-minions))
                [])
           (let [minions (-> (create-empty-state)
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i1") :position 0})
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i2") :position 1})
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i3") :position 2})
                             (add-minion-to-board {:player-id "p1" :minion (create-minion "Imp" :id "i4") :position 3})
                             (remove-minion "i2")
                             (get-minions "p1"))]
             (is= (map :id minions) ["i1" "i3" "i4"])
             (is= (map :position minions) [0 1 2])))}
  [state id]
  (let [owner-id (:owner-id (get-minion state id))
        position (:position (get-minion state id))]
    (as-> state $
          (update-in $
               [:players owner-id :minions]
               (fn [minions]
                 (remove (fn [m] (= (:id m) id)) minions)))
          (reduce (fn [s,m]
                    (update-minion s
                                   (:id m)
                                   :position
                                   (fn [p]
                                     (if (> p position)
                                       (dec p)
                                       p))))
                  $
                  (get-minions $ owner-id)))))

(defn add-minion-to-graveyard
  "Adds the newly dead (entire) minion to the graveyard."
  {:test (fn []
           (let [state (create-game [{:minions ["Imp" "War Golem"]}])]
             (is= (-> state
                      (add-minion-to-graveyard "m1")
                      (get-dead-minion "m1")
                      (:name))
                  "Imp")))}
  [state minion-id]
  (let [minion (get-minion state minion-id)]
    (if (some? (:graveyard state))
      (update state :graveyard (fn [dead-minions]
                                 (conj dead-minions minion)))
      (assoc state :graveyard [minion]))))

(defn remove-minions
  "Removes the minions with the given ids from the state."
  {:test (fn []
           (is= (as-> (create-game [{:minions [(create-minion "Imp" :id "i1")
                                               (create-minion "Imp" :id "i2")]}
                                    {:minions [(create-minion "Imp" :id "i3")
                                               (create-minion "Imp" :id "i4")]}]) $
                      (remove-minions $ "i1" "i4")
                      (get-minions $)
                      (map :id $))
                ["i2" "i3"]))}
  [state & ids]
  (reduce remove-minion state ids))

(defn get-opponent-id
  "Returns the other player"
  [state player-id]
  {:pre [(some? player-id)]}
  (->> (get-players state)
       (filter (fn [p] (not= (:id p) player-id)))
       (first)
       (:id)))

(defn create-game
  "Creates a game with the given deck, hand, minions (placed on the board), and heroes."
  {:test (fn []
           (is= (create-game) (create-empty-state))

           (is= (create-game [{:hero (create-hero "Anduin Wrynn")}])
                (create-game [{:hero "Anduin Wrynn"}]))

           (is= (create-game [{:minions [(create-minion "Imp")]}])
                (create-game [{:minions ["Imp"]}]))

           (is= (create-game [{:minions ["Imp"]
                               :secrets ["Snake Trap"]
                               :deck    ["War Golem"]
                               :hand    ["Defender"]}
                              {:hero "Anduin Wrynn"}]
                             :player-id-in-turn "p2")
                {:player-id-in-turn             "p2"
                 :players                       {"p1" {:id       "p1"
                                                       :mana     1
                                                       :max-mana 1
                                                       :deck     [{:entity-type :card
                                                                   :card-type   :minion
                                                                   :id          "c2"
                                                                   :name        "War Golem"
                                                                   :owner-id    "p1"}]
                                                       :hand     [{:entity-type :card
                                                                   :card-type   :minion
                                                                   :id          "c3"
                                                                   :name        "Defender"
                                                                   :owner-id    "p1"}]
                                                       :minions  [{:damage-taken                0
                                                                   :attack                      1
                                                                   ;; :added-to-board-time-id      2
                                                                   :entity-type                 :minion
                                                                   :name                        "Imp"
                                                                   :id                          "m1"
                                                                   :health                      1
                                                                   :position                    0
                                                                   :owner-id                    "p1"}]
                                                       :hero     {:name                        "Jaina Proudmoore"
                                                                  :id                          "h1"
                                                                  :entity-type                 :hero
                                                                  :damage-taken                0}
                                                       :secrets  [ {:entity-type :secret
                                                                  :name        "Snake Trap"
                                                                  :id          "s4"
                                                                  :owner-id    "p1"}]}
                                                 "p2" {:id       "p2"
                                                       :mana     1
                                                       :max-mana 1
                                                       :deck     []
                                                       :hand     []
                                                       :minions  []
                                                       :hero     {:name                        "Anduin Wrynn"
                                                                  :id                          "h2"
                                                                  :entity-type                 :hero
                                                                  :damage-taken                0}}}
                 :counter                       5
                 :minion-ids-summoned-this-turn []}))}
  ([data & kvs]
   {:post [(= 2 (count (vals (:players %))))]}
   (let [players-data (map-indexed (fn [index player-data]
                                     (assoc player-data :player-id (str "p" (inc index))))
                                   data)
         state (as-> (create-empty-state (map (fn [player-data]
                                                (cond (nil? (:hero player-data))
                                                      (create-hero "Jaina Proudmoore")

                                                      (string? (:hero player-data))
                                                      (create-hero (:hero player-data))

                                                      :else
                                                      (:hero player-data)))
                                              data)) $
                     (reduce (fn [state {player-id :player-id
                                         minions   :minions
                                         deck      :deck
                                         hand      :hand
                                         secrets   :secrets}]
                               (-> state
                                   (add-minions-to-board player-id minions)
                                   (add-cards-to-deck player-id deck)
                                   (add-cards-to-hand player-id hand)
                                   (add-secrets player-id secrets)
                                   ))
                             $
                             players-data))]
     (if (empty? kvs)
       state
       (apply assoc state kvs))))
  ([]
   (create-game [])))
