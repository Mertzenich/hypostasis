(ns hypostasis.digitalocean
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def TOKEN (System/getenv "TOKEN"))

(def SSH_KEY "2e:cb:2b:cf:a2:7b:96:2e:b8:35:2b:a8:c4:b1:54:4b")

(defn- del-last-char
  [string]
  (subs string
        0
        (- (count string) 1)))

(defn query
  "Query the Digital Ocean API"
  [& {:keys [method resource id body] :as options}]
  (let [request (method (str "https://api.digitalocean.com/v2/" resource "/" id)
                        {:accept :json
                         :headers {"Authorization" (str "Bearer" " " TOKEN)}
                         :content-type :json
                         :body (json/write-str body)})]

    (if (= method client/delete)
      (:body request)
      (-> (:body request)
          (json/read-str :key-fn keyword)
          (get
           (keyword (if (or id body)
                      (del-last-char resource) ; Access single resource if an ID is provided
                      resource)))))))

(defn list-droplets
  "List all droplets"
  []
  (query {:method client/get
          :resource "droplets"}))

(defn retrieve-droplet
  "Retrieve droplet information"
  [droplet-id]
  (query {:method client/get
          :resource "droplets"
          :id droplet-id}))

(defn list-firewalls
  "List all firewalls"
  []
  (query {:method client/get
          :resource
          "firewalls"}))

(defn retrieve-firewall
  "Retrieve droplet information"
  [firewall-id]
  (query {:method client/get
          :resource "firewalls"
          :id firewall-id}))

(defn active-droplet?
  "Check whether droplet is active"
  [droplet-id]
  (= (get (retrieve-droplet droplet-id) :status)
     "active"))

(defn create-droplet
  "Create a new droplet"
  [name environment]
  ;; [method resource id body]
  (query {:method client/post
          :resource "droplets"
          :body {:name name
                 :tags ["hypostasis"]
                 :region "sfo"
                 :size "s-1vcpu-512mb-10gb"
                 :image "ubuntu-22-04-x64"
                 :ssh_keys [SSH_KEY]
                 :user_data (apply str
                                   (concat '("#!/bin/bash\n")
                                           '("echo export HYPOSTASIS_READY=true >>/etc/environment\n")
                                           (map (fn [kv]
                                                  (str "echo export "
                                                       kv
                                                       " >>/etc/environment" "\n"))
                                                environment)))}}))

(defn delete-droplet
  "Delete an existing droplet"
  [droplet-id]
  (query {:method client/delete
          :resource "droplets"
          :id droplet-id}))

(defn create-firewall
  "Create a new firewall"
  [name droplet-id inbound-rules]
  (query {:method client/post
          :resource "firewalls"
          :body {:name name
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
                                   :destinations {:addresses ["0.0.0.0/0" "::/0"]}}]}}))

