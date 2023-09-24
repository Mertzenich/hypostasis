(ns hypostasis.driver.digital-ocean-driver
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hypostasis.driver.util.digitalocean :as ocean]
            [hypostasis.driver.driver :as remote]
            [clj-ssh.ssh :as ssh]
            [babashka.process :as proc]))

(def SSH_KEY "2e:cb:2b:cf:a2:7b:96:2e:b8:35:2b:a8:c4:b1:54:4b")

(defn active-droplet?
  "Check whether droplet is active"
  [droplet-id]
  (= (get (ocean/retrieve-droplet droplet-id) :status)
     "active"))

(defn ready-to-initialize?
  "Check whether server is ready to initialize"
  [session]
  (ssh/with-connection session
    (let [cmd (ssh/ssh session {:cmd "echo $HYPOSTASIS_READY"})]
      ;; (println cmd)
      (= (str/trim (cmd :out)) "true"))))

(defn exec-on-remote
  "Execute a process"
  [remote-ip exec]
  (proc/process {:err :inherit :shutdown proc/destroy-tree}
                (str "ssh root@"
                     remote-ip
                     " -o \"ServerAliveInterval 60\" \"" exec "\"")))

(defrecord DigitalOcean [name firewall env transfer init exec id-atom]
  remote/Driver
  (provision [_]
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
      (println "Creating server:" name)
      (while (not (active-droplet? droplet-id))
        (Thread/sleep 1000))
      (println "Server" name "has been created.")
      (reset! id-atom droplet-id)
      _))

  (initialize [_]
    (let [droplet    (ocean/retrieve-droplet @id-atom)
          droplet-ip (get-in droplet [:networks :v4 0 :ip_address])
          agent      (ssh/ssh-agent {})]

      (println "Initializing server...")
      (while (not (ready-to-initialize? (ssh/session agent
                                                     droplet-ip
                                                     {:username "root" :strict-host-key-checking :no})))
        (Thread/sleep 5000))

      (let [session    (ssh/session agent droplet-ip {:username "root" :strict-host-key-checking :no})]
        (ssh/with-connection session
          (println "TRANSFER" ":" transfer)
          (let [channel (ssh/ssh-sftp session)]
            (ssh/with-channel-connection channel
            ;; TODO: Add support for non-root accounts
              (ssh/sftp channel {} :cd "/root")
              (doseq [i (range (count transfer))]
                (let [file-name (get transfer i)
                      file-path (str "resources/" file-name)]
                  (ssh/sftp channel {} :put file-path file-name)))))

            ;; Perform initialization
          (doseq [i (range (count init))]
            (let [result (ssh/ssh session {:cmd (get init i) :out :stream})
                  input-stream (:out-stream result)
                  reader (io/reader input-stream)]
              (doall (for [line (line-seq reader)]
                       (println (str "[" (:name droplet) "]") "[INIT]" (str "[" (get init i) "]") line))))))))
    _)
  (execute [_]
    (let [droplet (ocean/retrieve-droplet @id-atom)
          droplet-ip (get-in droplet [:networks :v4 0 :ip_address])]
      (with-open [rdr (io/reader (:out (proc/process {:err :inherit
                                                      :shutdown proc/destroy-tree}
                                                     (str "ssh root@"
                                                          droplet-ip
                                                          " -o \"ServerAliveInterval 60\" \"" exec "\""))))]

        (binding [*in* rdr]
          (loop []
            (when-let [line (read-line)]
              (println (str "[" (:name droplet) "]") "[EXEC]" line)
              (recur))))))
    _))
