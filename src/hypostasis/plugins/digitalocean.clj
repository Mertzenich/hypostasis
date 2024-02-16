(ns hypostasis.plugins.digitalocean
  (:require [hypostasis.plugins.plugin :as pl]
            [hypostasis.plugins.digitalocean-utils :as util]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [babashka.process :as proc]
            [clojure.string :as str]
            [clj-ssh.ssh :as ssh]))

(defmacro info
  [name & args]
  `(timbre/info (str "[" ~name "]") ~@args))

(defn produce-do-fw-format
  "Produce the Digital Ocean firewall format from a config firewall entry"
  [fw]
  (if (get fw :source)
    (-> (assoc fw :sources {:addresses [(get fw :source)]})
        (dissoc :source))
    (assoc fw :sources {:addresses ["0.0.0.0/0" "::/0"]})))

(defn create-droplet
  "Perform droplet creation action"
  [token dname ssh-key]
  (util/create-droplet token
                       {:name dname
                        :tags ["hypostasis"]
                        :region "sfo"
                        :size "s-1vcpu-512mb-10gb"
                        :image "ubuntu-22-04-x64"
                        :ssh_keys [ssh-key]}))

(defn create-firewall
  "Create a new digital ocean firewall"
  [token name droplet-id inbound-rules]
  (util/create-firewall token
                        {:name name
                         :droplet_ids [droplet-id]
                                         ;; Default: [{:protocol "tcp", :ports "22", :sources {:addresses ["0.0.0.0/0" "::/0"]}}]
                         :inbound_rules inbound-rules
                         :user_data "carrotonastick"
                                         ;; Allow all outbound by default
                         :outbound_rules [{:protocol "icmp",
                                           :ports "0",
                                           :destinations {:addresses ["0.0.0.0/0" "::/0"]}}
                                          {:protocol "tcp",
                                           :ports "0",
                                           :destinations {:addresses ["0.0.0.0/0" "::/0"]}}
                                          {:protocol "udp",
                                           :ports "0",
                                           :destinations {:addresses ["0.0.0.0/0" "::/0"]}}]}))

(defn update-firewall
  "Perform an update to a digital ocean firewall"
  [token firewall-id new-rules]
  (let [firewall (util/retrieve-firewall token firewall-id)]
    (util/update-firewall token firewall-id {:name (:name firewall)
                                             :droplet_ids (:droplet_ids firewall)
                                             :inbound_rules (concat (:inbound_rules firewall) new-rules)
                                             :user_data (:user_data firewall)
                                             :outbound_rules (:outbound_rules firewall)})))

(defn env-add
  "Add server env configuration to remote server via ssh"
  [ip env]
  (let [agent (ssh/ssh-agent {})
        session (ssh/session agent ip {:username "root" :strict-host-key-checking :no})
        env-map (map #(str "echo export " (first %) "=" (second %) " >>/etc/environment;")
                     env)
        env-cmd (apply str env-map)]
    (ssh/with-connection session
      (ssh/ssh session {:cmd env-cmd :out :stream}))))

(defn transfer
  "Transfer files to remote server via scp"
  [ip files]
  (let [quoted-files (map #(str "\"" % "\"") files)
        files-str (str/join " " quoted-files)]
    (proc/process {:err :inherit
                   :shutdown proc/destroy-tree}
                  (str "scp -r " files-str " root@" ip ":~"))))

(defn run
  "Perform remote command via ssh"
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
  "Perform remote initialization via ssh"
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

(defrecord DigitalOcean [config]
  ;; Config is an atom [name plugin firewall env transfer init exec settings]
  pl/Plugin
  (provision [_]
    (let [cfg @config
          cfg-token (get-in cfg [:settings :token])
          cfg-name (get cfg :name)
          cfg-ssh-key (get-in cfg [:settings :ssh-key])
          droplet (create-droplet cfg-token
                                  cfg-name
                                  cfg-ssh-key)
          droplet-id (get droplet :id)
          concrete-fws (pl/fw-filter-abstract (:firewall cfg))
          do-fws (map produce-do-fw-format concrete-fws)
          firewall (create-firewall cfg-token cfg-name droplet-id do-fws)
          firewall-id (get firewall :id)]
      (info cfg-name "Server started provisioning")
      (-> config
          (pl/config-assoc :droplet-id droplet-id)
          (pl/config-assoc :firewall-id firewall-id))
      ;; Delay until provisioning is complete (droplet is active)
      (while (not (util/active-droplet? cfg-token droplet-id))
        (Thread/sleep 1000))
      (Thread/sleep 30000)
      ;; Update Server IP
      (.ip _)
      (info cfg-name "Server has been provisioned"))
    _)
  (destroy [_]
    (let [cfg @config
          cfg-name (get cfg :name)
          cfg-token (get-in cfg [:settings :token])
          droplet-id (get cfg :droplet-id)
          firewall-id (get cfg :firewall-id)]
      (util/delete-droplet cfg-token droplet-id)
      (util/delete-firewall cfg-token firewall-id)
      (info cfg-name "Server Destroyed"))
    _)
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
      (run cfg-name "EXEC" ip cfg-cmd)))
  (ip [_]
    (let [cfg @config
          cfg-token (get-in cfg [:settings :token])
          droplet-id (get cfg :droplet-id)]
      (if (get cfg :ip)
        (get cfg :ip)
        (get @(pl/config-assoc config :ip (get-in (util/retrieve-droplet cfg-token droplet-id)
                                                  [:networks :v4 0 :ip_address]))
             :ip))))

  (firewall-update [_ servers]
    (let [cfg @config
          cfg-name (get cfg :name)
          cfg-token (get-in cfg [:settings :token])
          firewall-id (:firewall-id cfg)
          abstract-fws (pl/fw-filter-concrete (:firewall cfg))]
      (->> (map #(assoc % :source (let [rule %
                                        name (:source rule)
                                        server (get servers name)
                                        cfg @(:config server)
                                        ip (get cfg :ip)]
                                    ip)) abstract-fws)
           (map produce-do-fw-format)
           (update-firewall cfg-token firewall-id))
      (info cfg-name "Server Firewalls Updated"))
    _))

(defn create-instance
  "Takes a server configuration and creates Digital Ocean instance"
  [config]
  (->DigitalOcean (atom config)))
