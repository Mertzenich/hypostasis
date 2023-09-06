(ns hypostasis.remote
  (:require [hypostasis.digitalocean :as ocean]
            [clj-ssh.ssh :as ssh]
            [clojure.string :as str]))

(def SSH_KEY "2e:cb:2b:cf:a2:7b:96:2e:b8:35:2b:a8:c4:b1:54:4b")

(defn active-droplet?
  "Check whether droplet is active"
  [droplet-id]
  (= (get (ocean/retrieve-droplet droplet-id) :status)
     "active"))

(defn provision
  "Provision a server from the server definition"
  [name firewall env]
  (let [firewall-def (map (fn [rule]
                            (assoc rule :sources {:addresses ["0.0.0.0/0" "::/0"]}))
                          firewall)
        droplet (ocean/create-droplet {:name name
                                       :tags ["hypostasis"]
                                       :region "sfo"
                                       :size "s-1vcpu-512mb-10gb"
                                       :image "ubuntu-22-04-x64"
                                       :ssh_keys [SSH_KEY]
                                       ;; TODO: Clean this up with some helper functions
                                       :user_data (apply str
                                                         (concat '("#!/bin/bash\n")
                                                                 '("echo export HYPOSTASIS_READY=true >>/etc/environment\n")
                                                                 (map (fn [kv]
                                                                        (str "echo export "
                                                                             kv
                                                                             " >>/etc/environment" "\n"))
                                                                      env)))})
        droplet-id (get droplet :id)
        firewall (ocean/create-firewall {:name name
                                         :droplet_ids [droplet-id]
                                         ;; Default: [{:protocol "tcp", :ports "22", :sources {:addresses ["0.0.0.0/0" "::/0"]}}]
                                         :inbound_rules firewall-def
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
                                                           :destinations {:addresses ["0.0.0.0/0" "::/0"]}}]})]
    (while (not (active-droplet? droplet-id)) (println "Creating server:" name) (Thread/sleep 1000))
    (println "Server" name "has been created.")
    droplet-id))

(defn ready-to-initialize?
  "Check whether server is ready to initialize"
  [session]
  (ssh/with-connection session
    (let [cmd (ssh/ssh session {:cmd "echo $HYPOSTASIS_READY"})]
      ;; (println cmd)
      (= (str/trim (cmd :out)) "true"))))

(defn initialize
  "Initialize server by executing commands"
  [droplet-id init]
  (let [droplet    (ocean/retrieve-droplet droplet-id)
        droplet-ip (get-in droplet [:networks :v4 0 :ip_address])
        agent      (ssh/ssh-agent {})]

    (while (not (ready-to-initialize? (ssh/session agent
                                                   droplet-ip
                                                   {:username "root" :strict-host-key-checking :no})))
      (println "Initializing server...")
      (Thread/sleep 5000))

    (let [session    (ssh/session agent droplet-ip {:username "root" :strict-host-key-checking :no})]
      (ssh/with-connection session
        ;; TODO: Probably switch to using env as a cmd prefix instead of writing to bash profile?
        (doseq [i (range (count init))]
          (let [result (ssh/ssh session {:cmd (get init i)})]
            (println (str "[CMD] \"" (get init i) "\": " (:out result)))))))))

;; (hypostasis.remote/initialize (ocean/retrieve-droplet 372657238) "foo")
