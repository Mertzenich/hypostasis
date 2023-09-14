(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [hypostasis.driver.digitaloceandriver :refer [->DigitalOcean]])
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

;; Stage #1
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
                ;; driver (->DigitalOcean (get-in servers [i :name])
                ;;                        (get-in servers [i :firewall])
                ;;                        (get-in servers [i :env])
                ;;                        (get-in servers [i :transfer])
                ;;                        (get-in servers [i :init]))
                driver (->DigitalOcean (get-in servers [i :name])
                                       (get-in servers [i :firewall])
                                       (get-in servers [i :env])
                                       (get-in servers [i :transfer])
                                       (get-in servers [i :init]))
                droplet-id (.provision driver)
                ;; droplet-id (remote/provision (get-in servers [i :name])
                ;;                              (get-in servers [i :firewall])
                ;;                              (get-in servers [i :env]))
                ]
            (println "Server is warming up...")
            (Thread/sleep 30000)          ; Waiting until server is warmed up, TODO: more elegant way of doing this
            (.initialize driver
                         droplet-id)
            ;; (remote/initialize droplet-id
            ;;                    (get-in servers [i :transfer])
            ;;                    (get-in servers [i :init]))
            ))))
