(ns hypostasis.digitalocean
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def TOKEN (System/getenv "TOKEN"))

(def SSH_KEY "2e:cb:2b:cf:a2:7b:96:2e:b8:35:2b:a8:c4:b1:54:4b")

(defn list-droplets
  []
  (let [request (client/get "https://api.digitalocean.com/v2/droplets?tag_name=hypostasis"
                            {:accept :json
                             :headers {"Authorization" (str "Bearer" " " TOKEN)}})]
    (-> (:body request)
        (json/read-str :key-fn keyword)
        (get :droplets))))

(defn retrieve-droplet
  "Retreive droplet information"
  [droplet-id]
  (let [request (client/get (str "https://api.digitalocean.com/v2/droplets/" droplet-id)
                            {:accept :json
                             :headers {"Authorization" (str "Bearer" " " TOKEN)}})]
    (-> (:body request)
        (json/read-str :key-fn keyword)
        (get :droplet))))

(defn active-droplet?
  "Check whether droplet is active"
  [droplet-id]
  (= (get (retrieve-droplet droplet-id) :status)
     "active"))

["token=15633825565" "enabled=false"]
(map (fn [kv]
       (str "echo export " kv " >>~/.bash_profile" "\n")))

(defn create-droplet
  "Create a new droplet"
  [name environment]
  (let [request (client/post "https://api.digitalocean.com/v2/droplets/"
                             {:accept :json
                              :headers {"Authorization" (str "Bearer" " " TOKEN)}
                              :content-type :json
                              :body (json/write-str {:name name
                                                     :tags ["hypostasis"]
                                                     :region "sfo"
                                                     :size "s-1vcpu-512mb-10gb"
                                                     :image "ubuntu-22-04-x64"
                                                     :ssh_keys [SSH_KEY]
                                                     :user_data (apply str
                                                                       (concat '("#!/bin/bash\n") (map (fn [kv]
                                                                                                         (str "echo export "
                                                                                                              kv
                                                                                                              " >>~/.bash_profile" "\n"))
                                                                                                       environment)))})})]
    (-> (:body request)
        (json/read-str :key-fn keyword)
        (get :droplet))))

(defn delete-droplet
  "Delete an existing droplet"
  [droplet-id]
  (let [request (client/delete (str "https://api.digitalocean.com/v2/droplets/" droplet-id)
                               {:accept :json
                                :headers {"Authorization" (str "Bearer" " " TOKEN)}
                                :content-type :json})]
    (:body request)))

(defn list-firewalls
  "List all firewalls"
  []
  (let [request (client/get "https://api.digitalocean.com/v2/firewalls"
                            {:accept :json
                             :headers {"Authorization" (str "Bearer" " " TOKEN)}})]
    (-> (:body request)
        (json/read-str :key-fn keyword)
        (get :firewalls))))

(defn retrieve-firewall
  "Access a firewall"
  [firewall-id]
  (let [request (client/get (str "https://api.digitalocean.com/v2/firewalls/" firewall-id)
                            {:accept :json
                             :headers {"Authorization" (str "Bearer" " " TOKEN)}})]
    (-> (:body request)
        (json/read-str :key-fn keyword)
        (get :firewall))))

(defn create-firewall
  "Create a new droplet"
  [name droplet-id inbound-rules]
  (let [request (client/post "https://api.digitalocean.com/v2/firewalls/"
                             {:accept :json
                              :headers {"Authorization" (str "Bearer" " " TOKEN)}
                              :content-type :json
                              :body (json/write-str {:name name
                                                     :droplet_ids [droplet-id]
                                                     ;; [{:protocol "tcp", :ports "22", :sources {:addresses ["0.0.0.0/0" "::/0"]}}]
                                                     :inbound_rules inbound-rules
                                                     ;; Allow all outbound by default
                                                     :outbound_rules [{:protocol "icmp",
                                                                       :ports "0",
                                                                       :destinations {:addresses ["0.0.0.0/0" "::/0"]}}
                                                                      {:protocol "tcp",
                                                                       :ports "0",
                                                                       :destinations {:addresses ["0.0.0.0/0" "::/0"]}}
                                                                      {:protocol "udp",
                                                                       :ports "0",
                                                                       :destinations {:addresses ["0.0.0.0/0" "::/0"]}}]})})]
    (-> (:body request)
        (json/read-str :key-fn keyword)
        (get :firewall))))
