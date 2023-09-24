(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [hypostasis.driver.digital-ocean-driver :refer [->DigitalOcean]])
  ;; (:import [hypostasis.driver.digitaloceandriver DigitalOcean])
  (:gen-class))

;; Check config last modified (.lastModified (io/as-file (io/resource "config.edn")))
(def config
  "Read server configuration"
  (-> "resources/config.edn"
      slurp
      edn/read-string))

;; Stage #1 - Provision Servers
;; Stage #2 - Initialize Servers
;; Stage #3 - Transfer Files
;; Stage #4 - Manage/Monitor Servers
;;            + Handle remote errors
;;            + Schedule tasks
;;            + Perform reboots
;; Stage #N - Destroy Servers

;; (defn config-provision
;;   "Provision servers based upon configuration
;;   Returns vector of droplet-ids
;;   i.e. [droplet-id-1 droplet-id-2 ... droplet-id-n]"
;;   [cfg-vec]
;;   (let [future-droplets (mapv )]))

;; (defn provision
;;   "Provision a server
;;   Takes a remote/Driver implementation, returns driver when complete"
;;   [driver]
;;   (.provision driver))

;; (defn initialize
;;   "Initialize a server
;;   Takes a remote/Driver implementation, returns driver when complete"
;;   [driver]
;;   (.initialize driver))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println args)
  (cond (some #{"--provision"} args)
        (->> (mapv #(future (let [driver (->DigitalOcean (:name %)
                                                         (:firewall %)
                                                         (:env %)
                                                         (:transfer %)
                                                         (:init %)
                                                         (:exec %)
                                                         (atom 0))]
                              (.provision driver)
                              (println "Server" (:name driver) "is warming up.")
                              (Thread/sleep 30000)
                              (.initialize driver)))
                   (get config :servers))
             ;; (mapv deref)
             (mapv #(future (.execute (deref %)))))))
