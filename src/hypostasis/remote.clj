(ns hypostasis.remote
  (:require [hypostasis.digitalocean :as ocean]
            [clj-ssh.ssh :as ssh]
            [clojure.string :as str]))

(defn provision
  "Provision a server from the server definition"
  [name firewall env]
  (let [firewall-def (map (fn [rule]
                            (assoc rule :sources {:addresses ["0.0.0.0/0" "::/0"]}))
                          firewall)
        droplet (ocean/create-droplet name env)
        droplet-id (get droplet :id)
        firewall (ocean/create-firewall name
                                        droplet-id
                                        firewall-def)]
    (while (not (ocean/active-droplet? droplet-id)) (println "Creating server:" name) (Thread/sleep 1000))
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
      (Thread/sleep 1000))

    (let [session    (ssh/session agent droplet-ip {:username "root" :strict-host-key-checking :no})]
      (ssh/with-connection session
        ;; TODO: Use provided init
        ;; TODO: Probably switch to using env as a cmd prefix instead of writing to bash profile
        (let [result (ssh/ssh session {:cmd "echo $token"})])
        (let [result (ssh/ssh session {:cmd "echo $token"})]
          (println "Token:" (result :out)))
        (let [result (ssh/ssh session {:cmd "echo $enabled"})]
          (println "Enabled: " (result :out)))))))

;; (hypostasis.remote/initialize (ocean/retrieve-droplet 372657238) "foo")
