(ns firestone.instrumentation
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as spec-test]
            [firestone.server :as server]
            [firestone.spec]))

(spec/fdef server/game-response
           :args (spec/cat :game-states :firestone.spec/game-states))

(spec-test/instrument `server/game-response)