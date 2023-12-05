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

<<<<<<< HEAD
(defn list-plugins
  []
  {:DigitalOcean digitalocean/create-instance
   :Vultr vultr/create-instance})
=======
(defn initialize
  "Initialize a server"
  [driver]
  (let [agent (ssh/ssh-agent {})
        ip (.ip driver)
        config (:config driver)
        name (:name config)
        env (:env config)
        transfer (:transfer config)
        init (:init config)]
    (println "Initializing server...")
    ;; Set Environmental Variables
    (let [session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})]
      (ssh/with-connection session
        (doseq [i (range (count env))]
          (ssh/ssh session {:cmd (str "echo export "
                                      (get env i)
                                      " >>/etc/environment") :out :stream}))))
    (let [session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})]
      (ssh/with-connection session
        (println "TRANSFER" ":" transfer)
        (let [channel (ssh/ssh-sftp session)]
          (ssh/with-channel-connection channel
            ;; TODO: Add support for non-root accounts
            (ssh/sftp channel {} :cd "/root")
            (doseq [i (range (count transfer))]
              (let [file-name (get transfer i)
                    file-path (str "servers/" name "/" file-name)]
                (println file-name file-path)
                (ssh/sftp channel {} :put file-path file-name)))))
            ;; Perform initialization
        (doseq [i (range (count init))]
          (let [result (ssh/ssh session {:cmd (get init i) :out :stream})
                input-stream (:out-stream result)
                reader (io/reader input-stream)]
            (doall (for [line (line-seq reader)]
                     (println (str "[" name "]") "[INIT]" (str "[" (get init i) "]") line))))))))
  driver)

(defn get-drivers-ips
  "Access ips of each driver"
  [drivers]
  (println "Type Drivers: " (type drivers))
  (println "Drivers: " drivers)
  (let [ips (atom [])]
    (doseq [d drivers]
      ;; (swap! ips assoc (:name (:config @d)) (.ip @d))
      (swap! ips conj (str (:name (:config @d)) "=" (.ip @d))))
    @ips))

(defn add-server-addresses
  "Add every server's address to the environment"
  [drivers]
  (let [env (get-drivers-ips drivers)]
    (doseq [d drivers]
      (let [driver @d
            agent (ssh/ssh-agent {})
            ip (.ip driver)
            session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})]
        (ssh/with-connection session
          (doseq [i (range (count env))]
            (ssh/ssh session {:cmd (str "echo export "
                                        (get env i)
                                        " >>/etc/environment") :out :stream}))))))
  drivers)

(defn update-server-firewalls
  "Add every server's address to the environment"
  [drivers]
  (let [ips (get-drivers-ips drivers)]
    (doseq [ip ips]
      (doseq [d drivers]
        (let [driver @d
              ip-clean (second (str/split ip #"="))]
          (.firewall-add driver {:protocol "tcp", :ports "1-65535", :sources {:addresses [ip-clean]}})
          (.firewall-add driver {:protocol "udp", :ports "1-65535", :sources {:addresses [ip-clean]}})))))
  drivers)

(defn execute
  "Execute remote command"
  [driver]
  (let [ip (.ip driver)
        config (:config driver)
        exec (:exec config)
        name (:name config)
        process (proc/process {:err :inherit
                               :shutdown proc/destroy-tree}
                              (str "ssh root@"
                                   ip
                                   " -o \"ServerAliveInterval 60\" \"" exec "\""))]
    (with-open [rdr (io/reader (:out process))]
      (binding [*in* rdr]
        (loop []
          (when-let [line (read-line)]
            (println (str "[" name "]") "[EXEC]" line)
            (recur))))))
  driver)

(defn launch
  "Automatically provision, initialize, and execute every server in the configuration"
  []
  (let [plugin-list (loader/list-plugins "plugins")]
    (->> (mapv #(future (let [plugin (loader/load-plugin "plugins" ((:plugin %) plugin-list))
                              driver ((:create plugin) %)]
                          ((:provision plugin) driver)
                          (println "Server" (:name %) "is warming up.")
                          (Thread/sleep 30000)
                          (initialize driver)
                          driver))
               (get-servers))
         (add-server-addresses)
         (update-server-firewalls)
         (mapv #(future (execute (deref %)))))))
>>>>>>> master

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
