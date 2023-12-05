(ns hypostasis.plugins.vultr
  (:require [hypostasis.plugins.plugin :as pl]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre]
            [babashka.process :as proc]
            [clojure.string :as str]
            [clj-ssh.ssh :as ssh]))

(defmacro info
  [name & args]
  `(timbre/info (str "[" ~name "]") ~@args))

(defn active-instance?
  "Check whether instance is active"
  [token id]
  (Thread/sleep 120000)
  (= 1 1)
  ;; TODO: Fix this
  ;; (let [instance (retrieve-instance token id)
  ;;       status (get instance "server_status")]
  ;;   (= status "ok"))
  )

(defn create-vultr-instance
  "Create vultr server instance"
  [token name ssh-key-id firewall-id]
  (json/read-str (:out (proc/shell (str "curl -s \"https://api.vultr.com/v2/instances\" "
                                        "-X POST "
                                        "-H \"Authorization: Bearer " token "\" "
                                        "-H \"Content-Type: application/json\" "
                                        "--data '{\"region\": \"ord\", \"plan\": \"vc2-1c-1gb\", \"label\": \"" name "\", \"os_id\": 2104, \"firewall_group_id\": \"" firewall-id "\", \"tags\": [\"hypostasis\"], \"sshkey_id\": [\"" ssh-key-id "\"]}'")
                                   {:out :string}))))

(defn- create-firewall-group
  "Create a vultr firewall group"
  [token name]
  (json/read-str (:out (proc/shell
                        (str "curl -s \"https://api.vultr.com/v2/firewalls\" "
                             "-X POST "
                             "-H \"Authorization: Bearer " token "\" "
                             "-H \"Content-Type: application/json\" "
                             "--data '{\"description\" : \"" name "\"}'")
                        {:out :string}))))

;; [{:protocol "tcp" :ports "22"}
;;  {:protocol "tcp" :ports "80"}]

(defn add-firewall-rule
  "Add a firewall rule to a group"
  [token firewall-group-id rule]
  (json/read-str (:out (proc/shell
                        (str "curl -s \"https://api.vultr.com/v2/firewalls/" firewall-group-id "/rules\" "
                             "-X POST "
                             "-H \"Authorization: Bearer " token "\" "
                             "-H \"Content-Type: application/json\" "
                             "--data '{\"ip_type\" : \"v4\", \"protocol\" : \"" (:protocol rule) "\", \"port\" : \"" (:ports rule) "\", \"subnet\" : \"0.0.0.0\", \"subnet_size\" : 0}'")
                        {:out :string}))))

(defn create-firewall
  "Create vultr firewall"
  [token name rules]
  (let [firewall-group (create-firewall-group token name)
        firewall-group-id (get-in firewall-group ["firewall_group" "id"])]
    (doseq [rule (conj rules {:protocol "tcp" :ports "22"})]
      (add-firewall-rule token firewall-group-id rule))
    firewall-group))

(defn delete-firewall
  "Delete vultr firewall"
  [token id]
  (proc/shell (str "curl -s \"https://api.vultr.com/v2/firewalls/" id "\" -X DELETE -H \"Authorization: Bearer " token "\"") {:out :string}))

(defn retrieve-instance
  "Retrieve a vultr server instance"
  [token id]
  (get (json/read-str (:out (proc/shell (str "curl -s \"https://api.vultr.com/v2/instances/" id "\" -X GET -H \"Authorization: Bearer " token "\"") {:out :string})))
       "instance"))

(defn delete-instance
  "Delete a vultr server instance"
  [token id]
  (proc/shell (str "curl -s \"https://api.vultr.com/v2/instances/" id "\" -X DELETE -H \"Authorization: Bearer " token "\"") {:out :string}))


(defn env-add
  "Add server env configuration to remote server"
  [ip env]
  (let [agent (ssh/ssh-agent {})
        session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})
        env-map (map #(str "echo export " (first %) "=" (second %) " >>/etc/environment;")
                     env)
        env-cmd (apply str env-map)]
    (ssh/with-connection session
      (ssh/ssh session {:cmd env-cmd :out :stream}))))

(defn transfer
  "Transfer files to remote server"
  [ip files]
  (let [quoted-files (map #(str "\"" % "\"") files)
        files-str (str/join " " quoted-files)]
    (proc/process {:err :inherit
                   :shutdown proc/destroy-tree}
                  (str "scp -r " files-str " root@" ip ":~"))))

(defn run
  "Perform remote command"
  [name prefix ip cmd]
  (let [process (proc/process {:err :inherit
                               :shutdown proc/destroy-tree}
                              (str "ssh root@"
                                   ip
                                   " -o \"ServerAliveInterval 60\" \"" cmd "\""))]
    (with-open [rdr (io/reader (:out process))]
      (binding [*in* rdr]
        (loop []
          (when-let [line (read-line)]
            (info name (str "[" prefix "]") line)
            (recur)))))))

(defn init
  "Perform remote initialization"
  [name ip init]
  ;; (let [cmd (str/join ";" init)]
  ;;   (run name "INIT" ip cmd))
  (let [cmd (str/join ";" init)
        agent (ssh/ssh-agent {})
        session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})]
    (ssh/with-connection session
      (let [result (ssh/ssh session {:cmd cmd :out :stream})
            input-stream (:out-stream result)
            reader (io/reader input-stream)]
        (doall (for [line (line-seq reader)]
                 (info name "[INIT]" line)))))))

(defrecord Vultr [config]
  ;; Config is an atom [name plugin firewall env transfer init exec settings]
  pl/Plugin
  (provision [_]
    (let [cfg @config
          cfg-name (:name cfg)
          cfg-token (:token (:settings cfg))
          cfg-ssh-key-id (:ssh-key-id (:settings cfg))
          concrete-fws (pl/fw-filter-abstract (:firewall cfg))
          firewall (create-firewall cfg-token cfg-name concrete-fws)
          firewall-id (get-in firewall ["firewall_group" "id"])
          instance (get (create-vultr-instance cfg-token cfg-name cfg-ssh-key-id firewall-id)
                        "instance")
          instance-id (get instance "id")]
      (info cfg-name "Server started provisioning")
      (-> config
          (pl/config-assoc :instance-id instance-id)
          (pl/config-assoc :firewall-id firewall-id))
      (while (not (active-instance? cfg-token instance-id))
        (Thread/sleep 1000))
      (.ip _)
      (info cfg-name "Server has been provisioned"))
    _)
  (destroy [_]
    (let [cfg @config
          cfg-name (get cfg :name)
          cfg-token (get-in cfg [:settings :token])
          instance-id (get cfg :instance-id)
          firewall-id (get cfg :firewall-id)]
      ;; (info cfg-name "Server Destroyed"))
    _))
  (initialize [_ servers]
    (let [cfg @config
          cfg-name (:name cfg)
          cfg-transfer (:transfer cfg)
          cfg-init (:init cfg)
          ip (.ip _)
          env (:env cfg)
          ip-env (map #(list (name (:name @(:config %))) (:ip @(:config %))) servers)]
      (env-add ip (concat env ip-env))
      (info cfg-name "Environment updated")
      (transfer ip cfg-transfer)
      (info cfg-name "Files transferred")
      (init cfg-name ip cfg-init)
      (info cfg-name "Initialization complete"))
    _)
  (execute [_]
    (let [cfg @config
          cfg-name (get cfg :name)
          cfg-cmd (get cfg :exec)
          ip (.ip _)]
      (run cfg-name "EXEC" ip cfg-cmd))
    _)
  (ip [_]
    (let [cfg @config
          cfg-token (get-in cfg [:settings :token])
          cfg-instance-id (:instance-id cfg)]
      (if (get cfg :ip)
        (get cfg :ip)
        (get @(pl/config-assoc config :ip (get (retrieve-instance cfg-token cfg-instance-id) "main_ip"))
             :ip))))
  (firewall-update [_ servers]
    _))

(defn create-instance [config]
  (->Vultr (atom config)))

;; (def inst (create-instance config))

;; (.provision inst)

;; (def servers [inst])

;; (.initialize inst servers)

;; (.execute inst)

;; (.ip inst)

;; (retrieve-instance (:token (:settings @(:config inst))) (:instance-id @(:config inst)))

;; (def cfg @config)
;; (def cfg-name (:name cfg))
;; (def cfg-token (:token (:settings cfg)))
;; (def cfg-ssh-key-id (:ssh-key-id (:settings cfg)))
;; (def concrete-fws (pl/fw-filter-abstract (:firewall cfg)))
;; (def firewall (create-firewall cfg-token cfg-name concrete-fws))
;; (def firewall-id (get-in firewall ["firewall_group" "id"]))
;; (def instance (get (create-vultr-instance cfg-token cfg-name cfg-ssh-key-id firewall-id)
;;                    "instance"))
;; (def instance-id (get instance "id"))
