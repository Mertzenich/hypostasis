(ns hypostasis.remote
  (:require [hypostasis.digitalocean :as ocean]
            [clj-ssh.ssh :as ssh]))

(defn provision
  "Provision a server from the server definition"
  [name firewall env]
  (let [firewall-def (map (fn [rule]
                            (assoc rule :sources {:addresses ["0.0.0.0/0" "::/0"]}))
                          firewall)
        droplet (ocean/create-droplet name env)
        firewall (ocean/create-firewall name
                                        (get droplet :id)
                                        firewall-def)]
    droplet))

(defn initialize
  "Initialize server by executing commands"
  [droplet init]
  (let [droplet-id (get droplet :id)]
    ;; Delay until droplet is active
    (while (not (ocean/active-droplet? droplet-id)) (println "Not active...") (Thread/sleep 1000))
    (println "Active!")
    (let [agent (ssh/ssh-agent {})
          session (ssh/session agent "198.211.114.160" {:username "root"})]
      (ssh/with-connection session
        (let [result (ssh/ssh session {:cmd "apt update"})]
          (println (result :out)))
        (let [result (ssh/ssh session {:cmd "apt -y upgrade"})]
          (println (result :out)))))))

;; (hypostasis.remote/initialize (ocean/retrieve-droplet 372657238) "foo")
