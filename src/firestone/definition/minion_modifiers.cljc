(ns firestone.definition.minion-modifiers
  (:require [ysera.test :refer [is is-not is= error?]]
            [ysera.collections :refer [seq-contains?
                                       remove-one]]
            [clojure.test :refer [function?]]
            [firestone.construct :refer [create-game
                                         create-minion
                                         create-hero
                                         update-minion

                                         get-minion
                                         get-hero]]
            [firestone.definitions :refer [get-definition]]
            [firestone.core :refer [get-character
                                    enough-mana?
                                    add-mana]]
            [firestone.definitions :refer [get-definition]]))


(defn increase-attack
  [state n minion]
  {:pre [(<= 0 n)]}
  (update-minion state (:id minion) :attack (partial + n)))

(defn decrease-attack
  [state n minion]
  {:pre [(<= 0 n)]}
  (if (< n (:attack minion))
    (update-minion state (:id minion) :attack (fn [dmg] (- dmg n)))
    (update-minion state (:id minion) :attack 0)))


(defn buff-entity-this-turn
  "_Temporarily_ updates the value of the given key for the entity with the given id.
   `buff-entity-this-turn` does NOT update the entity map directly!!
   The temporary buff is stored in :turn/:buffs. The buff is summed with the permanent
   value when get-minions is called.
   All values are cleared when end-turn is called."
  {:test (fn []
           (let [state (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                           (buff-entity-this-turn "i" :sleepy true))]
             (is= (-> (get-minion state "i")
                      (:sleepy))
                  true)
             (is= (-> (dissoc state :turn)
                      (get-minion "i")
                      (:sleepy)
                      (true?))
                  false))
           (let [state (-> (create-game [{:minions [(create-minion "Imp" :id "i")]}])
                           (buff-entity-this-turn "i" :attack 3))]
             (is= (-> (get-minion state "i")
                      (:attack))
                  4)
             (is= (-> (dissoc state :turn)
                      (get-minion "i")
                      (:sleepy)
                      (true?))
                  false)
             ;; buff-minion-this turn does NOT update the minion map directly!!
             (is= (-> state
                      (get-in [:player "p1" :attack]))
                  (-> (get-definition "Imp")
                      (:attack))))
           (is= (-> (create-game)
                    (buff-entity-this-turn "p1" :max-mana 1)
                    (add-mana 1 "p1")
                    (enough-mana? "p1" 2))
                true))}
  [state id key function-or-value]
  {:pre  [(some? function-or-value)]
   :post [(-> %
              (get-in [:turn :buffs id key])
              (some?))]}
  (update-in state [:turn :buffs id key] (if (function? function-or-value)
                                           function-or-value
                                           (constantly function-or-value))))

(defn set-character-state
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "Imp" :id "i")]}])]
             (is= (-> (set-character-state state "i" "STEALTH")
                      (get-minion "i")
                      (:char-states))
                  ["STEALTH"]))
           (let [state (create-game [{:minions [(create-minion "Blood Imp" :id "bi")]}])]
             (is= (-> (set-character-state state "bi" "FROZEN")
                      (get-minion "bi")
                      (:char-states))
                  ["STEALTH" "FROZEN"]))
           (let [state (create-game [{:hero (create-hero "Jaina Proudmoore" :id "p1")}])]
             (is= (-> (set-character-state state "p1" "FROZEN")
                      (get-hero "p1")
                      (:char-states))
                  ["FROZEN"])))}
  [state character-id new-state]
  {:pre [(get-character state character-id)]}
  (let [character (get-character state character-id)
        old-states (get-in character [:char-states])]
    (if (seq-contains? old-states new-state)
      state
      (if (= (:entity-type character) :minion)
        (update-minion state character-id :char-states
                       (conj (get-in character [:char-states]) new-state))
        (update-in state [:players (:owner-id character) :hero :char-states]
                   (fn [old-states] (conj old-states new-state)))))))

(defn remove-character-state
  {:test (fn []
           (let [state (create-game [{:minions [(create-minion "Blood Imp" :id "bi")]}])]
             (is= (-> (remove-character-state state "bi" "STEALTH")
                        (get-minion "bi")
                        (:char-states))
                  []))
           (let [state (create-game [{:minions [(create-minion "Imp" :id "i")]}])]
             (is= (remove-character-state state "i" "STEALTH")
                  state))
           (let [state (create-game [{:hero (create-hero "Jaina Proudmoore" :id "p1")}])]
             (is= (-> (set-character-state state "p1" "FROZEN")
                      (remove-character-state "p1" "FROZEN")
                      (get-hero "p1")
                      (:char-states))
                  [])))}
  [state character-id char-state]
  (let [character (get-character state character-id)
        old-states (get-in character [:char-states])]
  {:pre [(get-character state (:id character))
         (string? char-state)]}
  (if (seq-contains? (get-in character [:char-states]) char-state)
    (if (= (:entity-type character) :minion)
      (update-minion state (:id character) :char-states
                     (remove-one (get-in character [:char-states]) char-state))
      (update-in state [:players (:owner-id character) :hero :char-states]
                 (fn [old-states] (remove-one old-states char-state))))
    state)))

(defn increase-attack-this-turn
  [state n {minion-id :id}]
  {:pre [(<= 0 n)
         (get-minion state minion-id)]}
  (buff-entity-this-turn state minion-id :attack (fn [attack] (if (nil? attack) n (+ attack n)))))

(defn decrease-attack-this-turn
  [state n {minion-id :id}]
  {:pre [(<= 0 n)
         (get-minion state minion-id)]}
  (buff-entity-this-turn state minion-id :attack (fn [attack] (if (nil? attack) (- n) (- attack n)))))

(defn increase-health
  [state n minion]
  {:pre [(<= 0 n)]}
  (update-minion state (:id minion) :health (partial + n)))

