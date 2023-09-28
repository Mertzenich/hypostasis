(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [hypostasis.driver.digital-ocean-driver :refer [->DigitalOcean]]
            [hypostasis.driver.driver :as remote]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [babashka.fs :as fs])
  ;; (:import [hypostasis.driver.digitaloceandriver DigitalOcean])
  (:gen-class))

;; Check config last modified (.lastModified (io/as-file (io/resource "config.edn")))
;; (def config
;;   "Read server configuration"
;;   (-> "resources/config.edn"
;;       slurp
;;       edn/read-string))

(defn- get-servers-list
  "Return list of all server directories"
  []
  (filter #(.isDirectory (io/file %)) (map str (babashka.fs/list-dir "servers"))))

(defn- get-server-config
  "Access a server directory's configuration file"
  [dir]
  (edn/read-string (slurp (str dir "/server.edn"))))

(defn- fetch-servers
  "Fetch user-defined servers"
  []
  (map get-server-config (get-servers-list)))

(def config
  "Server configuration"
  (fetch-servers))

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

;; (def config (atom {}))

(defn- launch
  "Automatically provision, initialize, and execute every server in the configuration"
  []
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
             ;; (get config :servers)
             config)
             ;; (mapv deref)
       (mapv #(future (.execute (deref %))))))

(defn- create-default-config
  "Create default global configuration"
  []
  (spit "config.edn"
        (slurp (io/resource "config.edn"))))

(defn- create-default-server
  "Create default server configuration"
  []
  (.mkdirs (io/file "servers/default"))
  (doall
   (map #(spit % (slurp (io/resource %)))
        (vector "servers/default/server.edn" "servers/default/toinstall.txt" "servers/default/word.txt"))))

(defn- setup
  "Setup initial project structure"
  []
  (cond (not (.exists (io/file "servers")))
        (do (println "Creating config.edn")
            (create-default-config)))
  (cond (not (.exists (io/file "servers")))
        (do (println "Creating servers directory")
            (create-default-server))))

(def cli-options
  [["-h" "--help"]])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; (swap! config (fn [_] {:servers [{:name "Name of Server"
  ;;                                   :firewall [{:protocol "tcp" :ports "22"}
  ;;                                              {:protocol "tcp" :ports "80"}]
  ;;                                   :env ["TOKEN=15633825565"
  ;;                                         "ENABLED=false"
  ;;                                         "DEBIAN_FRONTEND=noninteractive"]
  ;;                                   :transfer ["toinstall.txt"
  ;;                                              "word.txt"]
  ;;                                   :init ["echo Token: $TOKEN"
  ;;                                          "echo Enabled: $ENABLED"
  ;;                                          "xargs apt-get -y <toinstall.txt"]
  ;;                                   :exec "cowsay <word.txt"}]}))
  (let [opts (parse-opts args cli-options)]
    (condp some (:arguments opts)
      #{"init"} :>> (setup)
      #{"run"} (launch)
      nil))
  ;; (cond (= nil args)
  ;;       (setup)
  ;;       (some #{"provision"} args)
  ;;       (launch))
  )
