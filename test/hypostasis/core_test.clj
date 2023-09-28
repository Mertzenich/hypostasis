(ns hypostasis.core-test
  (:require [clojure.test :refer :all]
            [hypostasis.core :refer :all]
            [clojure.java.io :as io]
            [babashka.fs :as fs])
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
        servers-dir (str test-dir "/servers")
        default-server (str servers-dir "/default")]
    (is (.exists (io/file config-edn)))
    (is (= false (.isDirectory (io/file config-edn))))
          ;; Servers directory creation
    (is (.exists (io/file servers-dir)))
    (is (= true (.isDirectory (io/file servers-dir))))
          ;; Default server created
    (is (= true (.exists (io/file default-server))))
    (is (= true (.isDirectory (io/file default-server))))
    (is (= true (.exists (io/file (str default-server "/server.edn")))))
    (is (= false (.isDirectory (io/file (str default-server "/server.edn")))))
    (is (= true (.exists (io/file (str default-server "/word.txt")))))
    (is (= false (.isDirectory (io/file (str default-server "/word.txt")))))
    (is (= true (.exists (io/file (str default-server "/toinstall.txt")))))
    (is (= false (.isDirectory (io/file (str default-server "/toinstall.txt")))))))

(deftest setup-test
  (let [test-dir (str (mk-tmp-dir!))]
    (testing "Setup"
      (testing "with nothing, without servers dir or config.edn"
        (let [setup-result (with-out-str (setup test-dir))]
          (setup-test-correct-structure test-dir)
          (is (= setup-result "Creating config.edn\nCreating servers directory\n"))))
      (testing "with config.edn and servers dir"
        (let [setup-result (with-out-str (setup test-dir))]
          (setup-test-correct-structure test-dir)
          (is (= "" setup-result))))
      ;; TODO
      (testing "with servers dir, without config.edn"
        (.delete (io/file (str test-dir "/config.edn")))
        (let [setup-result (with-out-str (setup test-dir))]
          (setup-test-correct-structure test-dir)
          (is (= "Creating config.edn\n" setup-result))))
      (testing "with config.edn, without servers dir"
        (fs/delete-tree (str test-dir "/servers"))
        (let [setup-result (with-out-str (setup test-dir))]
          (setup-test-correct-structure test-dir)
          (is (= "Creating servers directory\n" setup-result)))))))
