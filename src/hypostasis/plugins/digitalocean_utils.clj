(ns hypostasis.plugins.digitalocean-utils
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

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

(defn update-firewall
  "Update a firewall"
  [token id body]
  (query token {:method client/put :id id :resource "firewalls" :body body}))

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
