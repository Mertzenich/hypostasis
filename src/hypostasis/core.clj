(ns hypostasis.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clj-ssh.ssh :as ssh]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [hypostasis.loader :as loader])
  (:gen-class))

;;
;; SETUP SEQUENCE
;;

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

;;
;; LAUNCH SEQUENCE
;;

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
         (second (str/split dir #"/"))))

(defn get-servers
  "Server configuration"
  []
  (map get-server-config (list-servers "servers")))

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
    (println "IPS: " @ips)
    @ips))

(defn add-server-addresses
  "Add every server's address to the environment"
  [drivers]
  (doseq [d drivers]
    (let [driver @d
          agent (ssh/ssh-agent {})
          ip (.ip driver)
          env (get-drivers-ips drivers)
          session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})]
      (ssh/with-connection session
        (doseq [i (range (count env))]
          (ssh/ssh session {:cmd (str "echo export "
                                      (get env i)
                                      " >>/etc/environment") :out :stream})))))

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
         (mapv #(future (execute (deref %)))))))

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
