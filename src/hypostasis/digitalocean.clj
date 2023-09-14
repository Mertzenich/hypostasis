(ns hypostasis.digitalocean
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def TOKEN (System/getenv "TOKEN"))

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
(defmacro define-resource
  "Generate list, retrieve, create, and delete functions for a resource"
  [resource]
  ;; Drop the last character (s) for defining function names using singular name
  (let [short-resource (del-last-char resource)]
    `(do
       (defn ~(symbol (str "list" "-" resource))
         ~(str "List all " resource ".")
         []
         (query {:method client/get
                 :resource ~(str resource)}))

       (defn ~(symbol (str "retrieve" "-" short-resource))
         ~(str "Retreieve a " short-resource ".")
         [~'id]
         (query {:method client/get
                 :resource ~(str resource)
                 :id ~'id}))

       (defn ~(symbol (str "create" "-" short-resource))
         ~(str "Create a " short-resource ".")
         [~'body]
         (query {:method client/post
                 :resource ~(str resource)
                 :body ~'body}))

       (defn ~(symbol (str "delete" "-" short-resource))
         ~(str "Delete a " short-resource ".")
         [~'id]
         (query {:method client/delete
                 :resource ~(str resource)
                 :id ~'id})))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(declare list-droplets retrieve-droplet create-droplet delete-droplet
         list-firewalls retrieve-firewall create-firewall delete-firewall)

(define-resource "droplets")
(define-resource "firewalls")
