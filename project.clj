(defproject hypostasis "1.0.0-SNAPSHOT"
  :description "Programatic application infrastructure configuration and deployment."
  :url "https://github.com/mertad01/hypostasis"
  :license {}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.219"]
                 [org.clojure/data.json "2.4.0"]
                 [lambdaisland/kaocha "1.87.1366"]
                 [org.clj-commons/clj-ssh "0.6.2"]
                 [clj-http "3.12.3"]
                 [com.taoensso/timbre "6.3.1"]
                 [org.tcrawley/dynapath "1.1.0"]
                 [babashka/process "0.5.21"]
                 [babashka/fs "0.4.19"]
                 [clj-commons/pomegranate "1.2.23"]]
  :main hypostasis.core
  :target-path "target/%s"
  :plugins [[lein-codox "0.10.8"]]
  :codox {:exclude-vars nil
          :output-path "codox"}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
