(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [hypostasis.driver.digital-ocean-driver :refer [->DigitalOcean]]
            [hypostasis.driver.driver :as remote]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [babashka.fs :as fs])
  ;; (:import [hypostasis.driver.digitaloceandriver DigitalOcean])
  (:gen-class))

(defn- get-servers-list
  "Return list of all server directories"
  []
  (filter #(.isDirectory (io/file %)) (map str (babashka.fs/list-dir "servers"))))

(defn- get-server-config
  "Access a server directory's configuration file"
  [dir]
  (assoc (edn/read-string (slurp (str dir "/server.edn")))
         :name
         (second (str/split "servers/default" #"/"))))

(defn- fetch-servers
  "Fetch user-defined servers"
  []
  (map get-server-config (get-servers-list)))

(defn get-servers
  "Server configuration"
  []
  (fetch-servers))

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
             (get-servers))
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
  (let [opts (parse-opts args cli-options)]
    (condp some (:arguments opts)
      #{"init"} (setup)
      #{"run"} (launch)
      nil))
  )
