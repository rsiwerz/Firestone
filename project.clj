(defproject firestone "firestone"
  :description "dd2487 2018 firestone lab"
  :license {}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ysera "1.2.0"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot firestone.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
