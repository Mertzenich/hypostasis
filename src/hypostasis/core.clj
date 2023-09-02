(ns hypostasis.core
  (:require [hypostasis.digitalocean :as ocean]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:gen-class))

;; Check config last modified (.lastModified (io/as-file (io/resource "config.edn")))
(def config
  "Read server configuration"
  (-> "config.edn"
      io/resource
      slurp
      edn/read-string))

;; NOTE: Step 1
(defn provision-servers
  "Provision servers as defined by configuration, returns ..."
  [config]
  (let [cfg-servers (config :servers)]
    cfg-servers))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
