(ns hypostasis.core-test
  (:require [clojure.test :refer :all]
            [hypostasis.core :refer [setup list-plugins config-valid?]]
            [clojure.java.io :as io]
            [hypostasis.plugins.digitalocean :as digitalocean]
            [hypostasis.plugins.vultr :as vultr])
  (:import [java.io File]))

;; Credit to samaaron, micahasmith, and wilkerlucio for mk-tmp-dir!
;; https://gist.github.com/samaaron/1398198?permalink_comment_id=4163007#gistcomment-4163007
(defn mk-tmp-dir!
  "Creates a unique temporary directory on the filesystem. Typically in /tmp on
  *NIX systems. Returns a File object pointing to the new directory. Raises an
  exception if the directory couldn't be created after 10000 tries."
  []
  (let [base-dir     (io/file (System/getProperty "java.io.tmpdir"))
        base-name    (str (System/currentTimeMillis) "-" (long (rand 1000000000)) "-")
        tmp-base     (doto (File. (str base-dir "/" base-name)) (.mkdir))
        max-attempts 10000]
    (loop [num-attempts 1]
      (if (= num-attempts max-attempts)
        (throw (Exception. (str "Failed to create temporary directory after " max-attempts " attempts.")))
        (let [tmp-dir-name (str tmp-base num-attempts)
              tmp-dir      (io/file tmp-dir-name)]
          (if (.mkdir tmp-dir)
            tmp-dir
            (recur (inc num-attempts))))))))

(defn setup-test-correct-structure
  "Checks for correct setup structure"
  [test-dir]
  (let [config-edn (str test-dir "/config.edn")
        app-py (str test-dir "/app.py")
        main-py (str test-dir "/main.py")]
    (is (.exists (io/file config-edn)))
    (is (= false (.isDirectory (io/file config-edn))))
    (is (= false (.isDirectory (io/file app-py))))
    (is (= false (.isDirectory (io/file main-py))))))

(deftest setup-test
  (let [test-dir (str (mk-tmp-dir!))]
    (testing "Setup"
      (testing "with nothing, without servers dir or config.edn"
        (let [setup-result (with-out-str (setup test-dir))]
          (setup-test-correct-structure test-dir)
          (is (= setup-result "Creating config.edn\nCreating example app.py\nCreating example main.py\n")))))))

(deftest list-plugins-test
  (testing "List Plugins"
    (is (= (list-plugins) {:DigitalOcean digitalocean/create-instance :Vultr vultr/create-instance}))))

(deftest config-valid?-test
  (let [config [{:name :Primary
                 :plugin :DigitalOcean
                 :firewall [{:protocol "tcp" :ports "22"} ; No source, assume allow all i.e. 0.0.0.0
                            {:protocol "tcp" :ports "80"}
                            {:protocol "tcp" :ports "8000" :source :Alt}]
                 :env [["DEBIAN_FRONTEND" "noninteractive"]]
                 :transfer ["main.py"]
                 :init ["apt-get install -y python3-fastapi python3-uvicorn"
                        "ufw allow 8000/tcp"]
                 :exec "python3 -m uvicorn main:app --host 0.0.0.0"
                 :settings {:token "TOKEN HERE"
                            :ssh-key "SSH KEY HERE"}}
                {:name :Alt
                 :plugin :Vultr
                 :firewall [{:protocol "tcp" :ports "22"}
                            {:protocol "tcp" :ports "80"}
                            {:protocol "tcp" :ports "8000"}]
                 :env [["DEBIAN_FRONTEND" "noninteractive"]]
                 :transfer ["app.py"]
                 :init ["apt-get install -y python3-flask"
                        "ufw allow 8000/tcp"]
                 :exec "flask run --host 0.0.0.0 --port 8000"
                 :settings {:token "TOKEN HERE"
                            :ssh-key-id "SSH KEY HERE"}}]]
    (testing "Config validation"
      (is (config-valid? config) true))))
