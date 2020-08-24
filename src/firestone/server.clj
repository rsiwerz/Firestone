(ns firestone.server
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.data.json :as json]
            [firestone.edn-api :as edn-api]))


(defn game-response [game]
  {:status  200
   :headers {"Content-Type"                 "text/json; charset=utf-8"
             "Access-Control-Allow-Origin"  "*"
             "Access-Control-Allow-Methods" "*"}
   :body    (json/write-str game)})

(defn- decode-url [url]
  (java.net.URLDecoder/decode url))

(defn- get-params [request]
  (json/read-json (slurp (:body request))))


; @Tomas. The player-id of the acting player should be taken from login at a later stage.
; @Tomas. The game-id should be a part of the request.

(defn handle-request! [request]
  (let [uri (decode-url (:uri request))
        game-id "the-game-id"]
    (condp = uri

      "/createGame"
      (game-response (edn-api/create-game! game-id))

      "/endTurn"
      (let [player-id (edn-api/get-player-id-in-turn game-id)]
        (game-response (edn-api/end-turn! game-id player-id)))

      "/attack"
      (let [params (json/read-json (slurp (:body request)))
            attacker-id (:attackerId params)
            target-id (:targetId params)
            player-id (edn-api/get-player-id-in-turn game-id)]
        (game-response (edn-api/attack! game-id player-id attacker-id target-id)))

      "/playMinionCard"
      (let [params (json/read-json (slurp (:body request)))
            card-id (:cardId params)
            position (Integer. (:position params))
            player-id (edn-api/get-player-id-in-turn game-id)
            target-id (:targetId params)]
        (game-response (edn-api/play-minion-card! game-id player-id card-id position target-id)))

      "/playSpellCard"
      (let [params (json/read-json (slurp (:body request)))
            card-id (:cardId params)
            target-id (:targetId params)
            player-id (edn-api/get-player-id-in-turn game-id)]
        (game-response (edn-api/play-spell-card! game-id player-id card-id target-id)))

      "/useHeroPower"
      (let [params (json/read-json (slurp (:body request)))
            player-id (edn-api/get-player-id-in-turn game-id)
            target-id (:targetId params)]
        (game-response (edn-api/use-hero-power! game-id player-id target-id)))
      )))


(defonce server (atom nil))

;; Starting & Stopping

(defn stop! []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    (@server :timeout 100)
    (reset! server nil)))

(defn start! []
  (reset! server (run-server (fn [request] (handle-request! request))
                             {:port 8001})))

(comment
  (start!)
  (stop!))


