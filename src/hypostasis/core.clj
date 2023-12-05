(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [taoensso.timbre :as timbre]
            [hypostasis.loader :as loader]
            [hypostasis.plugins.digitalocean :as digitalocean]
            [hypostasis.plugins.vultr :as vultr])
  (:gen-class))

(defmacro info
  [name & args]
  `(timbre/info (str "[" ~name "]") ~@args))

(defn type-eq?
  "Returns true if type of 'v' is equal to type 't'"
  [v t]
  (= (type v) t))

(defn config-entry-verify
  "Returns true if server configuration 'sc' contains correctly typed values"
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
  [config]
  (every? config-entry-verify config))

(defn config-get
  []
  (let [config (edn/read-string (slurp "config.edn"))]
    (if (config-valid? config)
      config
      (throw (Exception. "Configuration file format invalid.")))))

(defn list-plugins
  []
  {:DigitalOcean digitalocean/create-instance
   :Vultr vultr/create-instance})

(list-plugins)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [configs (config-get)
        ;; list-plugins (loader/list-plugins "plugins")
        ;; servers (map #(do/->DigitalOcean (atom %)) configs)
        ;; servers (map #((loader/load-plugin "plugins" ((:plugin %) list-plugins)) %)
        ;;              configs)
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
    (mapv #(future (.execute %)) servers)
    ;; (doseq [s servers]
    ;;   (.execute s))
    ))

;; (def cfg (config-get))
;; (:plugin (first cfg))
;; (def list-plugins (loader/list-plugins "plugins"))
;; ((:plugin (first cfg)) list-plugins)
;; (loader/load-plugin "plugins" ((:plugin (first cfg)) list-plugins))

;; (let [list-plugins (loader/list-plugins "plugins")]
;;   (map #((loader/load-plugin "plugins" ((:plugin %) list-plugins)) %) cfg)
;;   )

;; (.provision (first *1))
;; (.destroy *1)
