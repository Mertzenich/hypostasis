(ns hypostasis.plugins.digitalocean.digitalocean
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [hypostasis.plugins.base :as base]))

(defn- del-last-char
  [string]
  (subs string
        0
        (- (count string) 1)))

(defn query
  "Query the Digital Ocean API"
  [token & {:keys [method resource id body] :as options}]
  (let [request (method (str "https://api.digitalocean.com/v2/" resource "/" id)
                        {:accept :json
                         :headers {"Authorization" (str "Bearer" " " token)}
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
  "List all droplets."
  [token]
  (query token {:method client/get, :resource "droplets"}))

(defn retrieve-droplet
  "Retreieve a droplet."
  [token id]
  (query token {:method client/get, :id id, :resource "droplets"}))

(defn create-droplet
  "Create a droplet."
  [token body]
  (query token {:method client/post, :resource "droplets", :body body}))

(defn delete-droplet
  "Delete a droplet."
  [token id]
  (query token {:method client/delete, :id id, :resource "droplets"}))

(defn list-firewalls
  "List all firewalls."
  [token]
  (query token {:method client/get, :resource "firewalls"}))

(defn retrieve-firewall
  "Retreieve a firewall."
  [token id]
  (query token {:method client/get, :id id, :resource "firewalls"}))

(defn create-firewall
  "Create a firewall."
  [token body]
  (query token {:method client/post, :resource "firewalls", :body body}))

(defn delete-firewall
  "Delete a firewall."
  [token id]
  (query token {:method client/delete, :id id, :resource "firewalls"}))

(defn active-droplet?
  "Check whether droplet is active"
  [token droplet-id]
  (= (get (retrieve-droplet token droplet-id) :status)
     "active"))

(defn- get-token
  "Retreive token from config defined environmental variable"
  [config]
  (System/getenv (get-in config [:settings :token-var])))

(defrecord DigitalOcean [config internal]
  base/Plugin
  (ip
    [_]
    (get-in (retrieve-droplet (get-token config) (:droplet-id @internal))
            [:networks :v4 0 :ip_address]))
  (provision
    [_]
    (let [firewall-def (map (fn [rule]
                              (assoc rule :sources {:addresses ["0.0.0.0/0" "::/0"]}))
                            (:firewall config))
          droplet (create-droplet (get-token config)
                                  {:name (:name config)
                                   :tags ["hypostasis"]
                                   :region "sfo"
                                   :size "s-1vcpu-512mb-10gb"
                                   :image "ubuntu-22-04-x64"
                                   :ssh_keys [(get-in config [:settings :ssh-key])]})
          droplet-id (:id droplet)
          firewall (create-firewall (get-token config)
                                    {:name (:name config)
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
      (swap! internal
             assoc
             :droplet-id droplet-id
             :firewall-id (:id firewall))

      (while (not (active-droplet? (get-token config) droplet-id))
        (Thread/sleep 1000))
      _))
  (destroy
    [_]
    (list (delete-firewall (get-token config) (:firewall-id @internal))
          (delete-droplet (get-token config) (:droplet-id @internal)))))

(defn create
  "Create a DigitalOcean plugin instance"
  [config]
  (->DigitalOcean config (atom {})))

(defn ip
  "Access the ip address"
  [driver]
  (.ip driver))

(defn provision
  "Provision the server"
  [driver]
  (.provision driver))

(defn destroy
  "Destroy the server"
  [driver]
  (.destroy driver))
