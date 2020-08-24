(ns firestone.mapper
  (:require [ysera.test :refer [is is=]]
            [clojure.spec.alpha :as s]
            [firestone.spec]
            [firestone.api-functions :refer [get-player-id-in-turn
                                             get-player-ids
                                             get-valid-attack-ids]]
            [firestone.core :refer [get-health
                                    get-mana
                                    get-max-health
                                    sleepy?
                                    can-attack?
                                    playable?
                                    can-use-hero-power?
                                    get-deck
                                    get-hand
                                    hero-power
                                    have-used-hero-power-this-turn?
                                    get-max-mana
                                    get-character-states]]
            [firestone.definitions :refer [get-definition]]
            [firestone.events.queue :refer [qflatten]]
            [firestone.construct :refer [get-heroes
                                         get-minion
                                         get-minions
                                         get-secrets
                                         get-hero]]))


(defn core-set->client-set
  [set]
  (get {:basic                        "basic"
        :classic                      "classic"
        :hall-of-fame                 "hall-of-fame"
        :curse-of-naxxramas           "curse-of-naxxramas"
        :goblins-vs-gnomes            "goblins-vs-gnomes"
        :blackrock-mountain           "blackrock-mountain"
        :the-grand-tournament         "the-grand-tournament"
        :the-league-of-explorers      "the-league-of-explorers"
        :whispers-of-the-old-gods     "whispers-of-the-old-gods"
        :one-night-in-karazhan        "one-night-in-karazhan"
        :mean-streets-of-gadgetzan    "mean-streets-of-gadgetzan"
        :journey-to-un'goro           "journey-to-un'goro"
        :knights-of-the-frozen-throne "knights-of-the-frozen-throne"
        :kobolds-and-catacombs        "kobolds-and-catacombs"
        :the-witchwood                "the-witchwood"
        :the-boomday-project          "the-boomday-project"}
       set))

(defn core-entity-type->client-entity-type
  [entity-type]
  (get {:card       "card"
        :hero       "hero"
        :hero-power "hero-power"
        :minion     "minion"
        :permanent  "permanent"
        :player     "player"
        :quest      "quest"
        :secret     "secret"
        :weapon     "weapon"}
       entity-type))

(defn core-type->client-type
  [type]
  (get {:hero       "hero"
        :hero-power "hero-power"
        :weapon     "weapon"
        :minion     "minion"
        :spell      "spell"}
       type))

(defn core-class-type->client-class-type
  [class]
  (get {:druid    "druid"
        :hunter   "hunter"
        :mage     "mage"
        :paladin  "paladin"
        :priest   "priest"
        :rogue    "rogue"
        :shaman   "shaman"
        :warlock  "warlock"
        :warrior  "warrior"}
       class))

(defn core-rarity->client-rarity
  [rarity]
  (get {:none       "none"
        :common     "common"
        :rare       "rare"
        :epic       "epic"
        :legendary  "legendary"}
       rarity))

(defn core-id->client-id
  [id]
  (str id))

(defn core-minion->client-minion
  [state minion-id]
  (let [minion (get-minion state minion-id)
        owner (:owner-id minion)
        valid-attack-ids (get-valid-attack-ids state minion-id)
        can-attack (if (empty? valid-attack-ids)
                     false
                     (can-attack? state minion-id))
        minion-def (get-definition (:name minion))]
    {:attack           (:attack minion)
     :can-attack       can-attack
     :entity-type      (core-entity-type->client-entity-type (:entity-type minion))
     :health           (get-health state minion-id)
     :id               (core-id->client-id minion-id)
     :name             (:name minion)
     :mana-cost        (:mana-cost minion-def)
     :max-health       (get-max-health state minion-id)
     :original-attack  (:base-attack minion-def)
     :original-health  (:health minion-def)
     :owner-id         (core-id->client-id owner)
     :position         (:position minion)
     :set              (core-set->client-set (:set minion-def))
     :sleepy           (sleepy? state minion-id)
     :states           (get-character-states minion)
     :valid-attack-ids valid-attack-ids
     :description      (if (contains? minion-def :description)
                         (:description minion-def)
                         "")}))

(defn  core-board-entities->client-board-entities
  [state player-id]
  (map (fn [m] (core-minion->client-minion state (:id m)))
       (sort-by :position (get-minions state player-id))))

(defn core-secret->client-secret
  [secret]
  (let [secret-def (get-definition (:name secret))]
    {:name                (:name secret)
     :owner-id            (core-id->client-id (:owner-id secret))
     :class               (core-class-type->client-class-type (:class secret-def))
     :id                  (core-id->client-id (:id secret))
     :entity-type         (core-entity-type->client-entity-type (:entity-type secret))
     :rarity              (core-rarity->client-rarity (:rarity secret-def))
     :original-mana-cost  (:mana-cost secret-def)
     :description         (:description secret-def)}))

(defn core-active-secrets->client-active-secrets
  [state player-id]
  (map core-secret->client-secret
       (get-secrets state player-id)))

(defn core-card->client-card
  [state player-id card]
  {:post [(s/valid? :firestone.spec/card %)]}
  (let [card-def (get-definition (:name card))]
    (cond-> {:entity-type        (core-entity-type->client-entity-type (:entity-type card))
             :name               (:name card)
             :mana-cost          (:mana-cost card-def)
             :original-mana-cost (:mana-cost card-def)
             :type               (core-type->client-type (:type card-def))
             :id                 (core-id->client-id (:id card))
             :owner-id           (core-id->client-id player-id)}
      (contains? card-def :description) (assoc :description (:description card-def))
      (contains? card-def :valid-targets) (assoc :valid-target-ids ((:valid-targets card-def) state))
      (playable? state player-id card-def) (assoc :playable true))))

(defn core-hand->client-hand
  [state player-id]
  (map (partial core-card->client-card state player-id)
       (get-hand state player-id)))

(defn core-hero-power->client-hero-power
  [state player-id]
  (let [hero (get-hero state player-id)
        hero-power (hero-power (:name hero))]
    {:can-use            (can-use-hero-power? state player-id)
     :owner-id           player-id
     :entity-type        (core-entity-type->client-entity-type :hero-power)
     :has-used-your-turn (have-used-hero-power-this-turn? state player-id)
     :description        (:description hero-power)
     :name               (:name hero-power)
     :mana-cost          (:mana-cost hero-power)
     :original-mana-cost (:mana-cost hero-power)
     :valid-target-ids   (if (contains? hero-power :valid-targets)
                           ((:valid-targets hero-power) state)
                           #{})}))

(defn core-hero->client-hero
  [state player-id]
  (let [hero (get-hero state player-id)]
    {:armor             0
     :owner-id          (core-id->client-id player-id)
     :entity-type       (core-entity-type->client-entity-type (:entity-type hero))
     :attack            0
     :can-attack        (can-attack? state player-id)
     :health            (get-health hero)
     :id                (core-id->client-id (:id hero))
     :mana              (get-mana state player-id)
     :max-health        (get-max-health state player-id)
     :max-mana          (get-max-mana state player-id)
     :name              (:name hero)
     :states            (get-character-states hero)
     :valid-attack-ids  (get-valid-attack-ids state (:id hero))
     :hero-power        (core-hero-power->client-hero-power state player-id)}))

(defn core-player->client-player
  [state player-id]
  {:board-entities (core-board-entities->client-board-entities state player-id)
   :active-secrets (core-active-secrets->client-active-secrets state player-id)
   :hand           (core-hand->client-hand state player-id)
   :deck-size      (count (get-deck state player-id))
   :hero           (core-hero->client-hero state player-id)
   :id             (core-id->client-id player-id)})

(defn core-state->client-event
  "Takes the event queue and returns a event for the view."
  [state]
  (let [event (-> (get-in state [:turn :event-log])
                  (qflatten)
                  (last))]
    (cond->
      {}
      (= (:event-type event)
        :damage-minion)
      (merge (let [target (get-minion state (:minion-id event))
                   id (:id target)
                   damage (:damage event)]
               {:name                 "damage"
                :character-amount-seq [{:character id
                                        :amount    damage}]})))))

(defn core-state->client-state
  [state]
  {:pre [(= 2 (-> (:players state)
                  (vals)
                  (count)))]}
  (cond-> {:id             (-> state
                               :game-id
                               (core-id->client-id))
           :player-in-turn (-> state
                               (get-player-id-in-turn)
                               (core-id->client-id))
           :players        (map (fn [player-id]
                           (core-player->client-player state player-id))
                                (get-player-ids state))}
          (not (empty? (core-state->client-event state))) (assoc :event (core-state->client-event state))))

(defn core-game->client-game
  [states]
  (->> (rest states)
       (mapv core-state->client-state)))