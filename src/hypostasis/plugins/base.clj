(ns hypostasis.plugins.base)

(defprotocol Plugin
  "Remote server provider implementation"
  (ip [this] "Access remote server IP")
  (provision [this] "Provision a remote server")
  (destroy [this] "Destroy a remote server"))
