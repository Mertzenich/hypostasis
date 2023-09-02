(defproject hypostasis "0.1.0-SNAPSHOT"
  :description "Programatic application infrastructure configuration and deployment."
  :url "https://github.com/mertad01/hypostasis"
  :license {}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clj-commons/clj-ssh "0.6.2"]]
  :main ^:skip-aot hypostasis.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
