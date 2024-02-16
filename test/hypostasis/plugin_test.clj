(ns hypostasis.plugin-test
  (:require [clojure.test :refer :all]
            [hypostasis.plugins.plugin :refer [fw-filter-abstract fw-filter-concrete config-assoc config-dissoc]]))

(def firewall [{:protocol "tcp" :ports "22"}
               {:protocol "tcp" :ports "8000" :source :Alt}])

(deftest fw-filter-abstract-test
  (testing "Firewall Filter Abstract"
    (is (= (fw-filter-abstract firewall) [{:protocol "tcp" :ports "22"}]))))

(deftest fw-filter-concrete-test
  (testing "Firewall Filter Concrete"
    (is (= (fw-filter-concrete firewall) [{:protocol "tcp" :ports "8000" :source :Alt}]))))

(deftest config-assoc-test
  (let [config (atom {})
        assoc1 (config-assoc config :foo "bar")
        deref1 @config]
    (testing "Config Assoc"
      (is (= config assoc1))
      (is (= (:foo deref1) "bar")))))


(deftest config-dissoc-test
  (let [config (atom {:foo "bar"})
        dissoc1 (config-dissoc config :foo)
        deref1 @config]
    (testing "Config Dissoc"
      (is (= config dissoc1))
      (is (= (:foo deref1) nil)))))
