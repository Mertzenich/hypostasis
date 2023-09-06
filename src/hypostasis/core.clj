(ns hypostasis.core
  (:require [hypostasis.digitalocean :as ocean]
            [hypostasis.remote :as remote]
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

;; NOTE: Stage #1
(defn provision-servers
  "Provision servers as defined by configuration, returns ..."
  [config]
  (let [cfg-servers (config :servers)]
    cfg-servers))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println args)
  (cond (some #{"--provision"} args)
        (doseq [i (range (count (get config :servers)))]

          (let [servers (get config :servers)
                droplet-id (remote/provision (get-in servers [i :name])
                                             (get-in servers [i :firewall])
                                             (get-in servers [i :env]))]
            (println "Server is warming up...")
            (Thread/sleep 30000)          ; Waiting until server is warmed up, TODO: more elegant way of doing this
            (hypostasis.remote/initialize droplet-id
                                          (get-in servers [i :init]))))
        ))
