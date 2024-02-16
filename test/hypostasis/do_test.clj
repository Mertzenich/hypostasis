(ns hypostasis.do-test
  (:require  [clojure.test :refer :all]
             [hypostasis.plugins.digitalocean :refer [produce-do-fw-format]]))

(def config-firewall-entry {:protocol "tcp" :ports "22"})

(deftest produce-do-fw-format-test
  (testing "Produce Digital Ocean Firewall Format"
    (is (= (produce-do-fw-format config-firewall-entry)
           {:protocol "tcp", :ports "22", :sources {:addresses ["0.0.0.0/0" "::/0"]}}))))
