(ns hypostasis.plugins.plugin)

(defprotocol Plugin
  "Remote server plugin protocol, must be implemented by plugins.
  Server plugins must also provide a create-instance function
  in addition to implementing the protocol."
  (provision [this] "Provision a new server")
  (destroy [this] "Destroy the server")
  (initialize [this servers] "Initialize the server")
  (execute [this] "Execute server command")
  (ip [this] "Access the server address")
  (firewall-update [this servers-map] "Update firewall using abstract definitions"))

(defn fw-filter-abstract
  "Takes firewall entry list fw and filters out abstract entries"
  [fws]
  (filter #(not (keyword? (:source %))) fws))

(defn fw-filter-concrete
  "Takes firewall entry list fw and filters out concrete entries"
  [fws]
  (filter #(keyword? (:source %)) fws))

(defn config-assoc
  "Takes a config atom and performs an assoc operation using provided key and value,
  returns the config atom"
  [config & kvs]
  (apply swap! config assoc kvs)
  config)

(defn config-dissoc
  "Takes a config atom and performs a dissoc operation using the provided key,
  returns the config atom"
  [config key]
  (swap! config dissoc key)
  config)

