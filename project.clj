(defproject vulture "0.1.0-SNAPSHOT"
  :description "experiments with RaptorQ"
  :license {:name "ISC"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [crypto-random "1.2.1"]]
  :main ^:skip-aot vulture.core
  :target-path "target/%s"
  :resource-paths ["resources/openrq-3.3.2.jar"]
  :profiles {:uberjar {:aot :all}})
