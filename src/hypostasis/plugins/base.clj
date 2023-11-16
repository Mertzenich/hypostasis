(ns hypostasis.plugins.base)

(defprotocol Plugin
  "Remote server provider implementation"
  (ip [this] "Access remote server IP")
  (provision [this] "Provision a remote server")
  (firewall-add [this new])
  (destroy [this] "Destroy a remote server"))
