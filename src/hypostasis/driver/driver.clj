(ns hypostasis.driver.driver)

(defprotocol Driver
  "Driver for provisioning and initializing a server"
  (provision [this])
  (initialize [this droplet-id]))
