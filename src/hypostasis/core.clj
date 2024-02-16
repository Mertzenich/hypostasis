(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [hypostasis.plugins.digitalocean :as digitalocean]
            [hypostasis.plugins.vultr :as vultr])
  (:gen-class))

(defmacro info
  "Simpligy timbre/info logs in the server format"
  [name & args]
  `(timbre/info (str "[" ~name "]") ~@args))

(defn type-eq?
  "Returns true if type of 'v' is equal to type 't'"
  [v t]
  (= (type v) t))

(defn config-entry-verify
  "Takes an individual server definition.
  Returns true if server configuration 'sc'
  contains correctly typed values"
  [sc]
  (and
   ;; Name: Keyword
   (type-eq? (:name sc) clojure.lang.Keyword)
   ;; Plugin: Keyword
   (type-eq? (:plugin sc) clojure.lang.Keyword)
   ;; Firewall: Vector
   (type-eq? (:firewall sc) clojure.lang.PersistentVector)
   ;; Firewall Rule: Keyword/Map
   (every? #(type-eq? % clojure.lang.PersistentArrayMap) (:firewall sc))
   ;; Env: Vector
   (type-eq? (:env sc) clojure.lang.PersistentVector)
   ;; Env Entry: Vector
   (every? #(and (type-eq? % clojure.lang.PersistentVector)
                 (type-eq? (first %) java.lang.String)
                 (type-eq? (second %) java.lang.String)) (:env sc))
   ;; Transfer: Vector
   (type-eq? (:transfer sc) clojure.lang.PersistentVector)
   ;; Transfer Entry: String
   (every? #(type-eq? % java.lang.String) (:transfer sc))
   ;; Init: Vector
   (type-eq? (:init sc) clojure.lang.PersistentVector)
   ;; Init Entry: String
   (every? #(type-eq? % java.lang.String) (:init sc))
   ;; Exec: String
   (type-eq? (:exec sc) java.lang.String)
   ;; Settings: Map
   (type-eq? (:settings sc) clojure.lang.PersistentArrayMap)))

(defn config-valid?
  "Takes a config and returns true if every individual
  server configuration is valid."
  [config]
  (every? config-entry-verify config))

(defn config-get
  "Read config.edn in the current directory and check for validity"
  []
  (let [config (edn/read-string (slurp "config.edn"))]
    (if (config-valid? config)
      config
      (throw (Exception. "Configuration file format invalid.")))))

(defn list-plugins
  "Return a map containing all of the plugin keyword names
  matched to their create-instance functions"
  []
  {:DigitalOcean digitalocean/create-instance
   :Vultr vultr/create-instance})

(def cli-options
  [["-h" "--help"]])

(defn setup
  "Setup initial project structure using jar resources.
  Takes a string 'dir' and places configuration files in
  that location."
  [dir]
  (let [config-edn (str dir "/config.edn")
        app-py (str dir "/app.py")
        main-py (str dir "/main.py")]
    (cond (not (.exists (io/file config-edn)))
          (do (println "Creating config.edn")
              (spit config-edn
                    (slurp (io/resource "config.edn")))))
    (cond (not (.exists (io/file app-py)))
          (do (println "Creating example app.py")
              (spit app-py
                    (slurp (io/resource "app.py")))))
    (cond (not (.exists (io/file main-py)))
          (do (println "Creating example main.py")
              (spit main-py
                    (slurp (io/resource "main.py")))))
    true))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (condp some (:arguments opts)
      #{"init"} (setup ".")
      #{"run"} (let [configs (config-get)
                     servers (mapv #(((:plugin %) (list-plugins)) %) configs)
                     server-config-map (reduce #(assoc %1 (:name @(:config %2)) %2)
                                               {}
                                               servers)]
                 (let [runtime (Runtime/getRuntime)]
                   (.addShutdownHook runtime
                                     (Thread. (fn []
                                                (doseq [s servers] (.destroy s))))))
                 (doseq [s servers]
                   (.provision s))
                 (doseq [s servers]
                   (.firewall-update s server-config-map))
                 (doseq [s servers]
                   (.initialize s servers))
                 (mapv #(future (.execute %)) servers))
      nil)))
