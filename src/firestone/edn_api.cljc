(ns firestone.edn-api
  (:require [firestone.api-functions :as api-functions :refer [new-game
                                                               end-turn
                                                               play-minion-card
                                                               attack
                                                               use-hero-power
                                                               play-spell-card]]
            [firestone.mapper :refer [core-game->client-game]]))

(defonce state-atom (atom nil))

(defn get-player-id-in-turn
  "This function is NOT pure!"
  [game-id]
  (-> (deref state-atom)
      (last)
      (api-functions/get-player-id-in-turn)))

(defn create-game! [game-id]
  (swap! state-atom last)
  (core-game->client-game (reset! state-atom (new-game game-id))))

(defn attack! [game-id player-id attacker-id target-id]
  (swap! state-atom last)
  (core-game->client-game (swap! state-atom attack game-id player-id attacker-id target-id)))

(defn end-turn! [game-id player-id]
  (swap! state-atom last)
  (core-game->client-game (swap! state-atom end-turn player-id)))

(defn play-minion-card! [game-id player-id card-id position target-id]
  (swap! state-atom last)
  (core-game->client-game (swap! state-atom play-minion-card game-id player-id card-id position target-id)))

(defn play-spell-card! [game-id player-id card-id target-id]
  (swap! state-atom last)
  (core-game->client-game (swap! state-atom play-spell-card game-id player-id card-id target-id)))

(defn use-hero-power! [game-id player-id target-id]
  (swap! state-atom last)
  (core-game->client-game (swap! state-atom use-hero-power game-id player-id target-id)))