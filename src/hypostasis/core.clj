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

(defn list-servers
  "Return list of all server directories"
  [servers-dir]
  (filter #(.isDirectory (io/file %))
          (map str (babashka.fs/list-dir servers-dir))))

(defn get-server-config
  "Access a server directory's configuration file"
  [dir]
  (assoc (edn/read-string (slurp (str dir "/server.edn")))
         :name
         (second (str/split "servers/default" #"/"))))

(defn get-servers
  "Server configuration"
  []
  (map get-server-config (list-servers "servers")))

(defn launch
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

(defn create-default-config
  "Create default global configuration"
  [dir]
  (spit (str dir "/config.edn")
        (slurp (io/resource "config.edn"))))

(defn create-default-server
  "Create default server configuration"
  [dir]
  (.mkdirs (io/file (str dir "/servers/default")))
  (doall
   (map #(spit (str dir "/" %) (slurp (io/resource %)))
        (vector "servers/default/server.edn" "servers/default/toinstall.txt" "servers/default/word.txt"))))

(defn setup
  "Setup initial project structure"
  [dir]
  (let [config-edn (str dir "/config.edn")
        servers-dir (str dir "/servers")]
    (cond (not (.exists (io/file config-edn)))
          (do (println "Creating config.edn")
              (create-default-config dir)))
    (cond (not (.exists (io/file servers-dir)))
          (do (println "Creating servers directory")
              (create-default-server dir)))
    [config-edn servers-dir]))

(def cli-options
  [["-h" "--help"]])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (condp some (:arguments opts)
      #{"init"} (setup ".")
      #{"run"} (launch)
      nil)))
