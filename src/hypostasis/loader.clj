(ns hypostasis.loader
  (:require [babashka.fs :as fs]
            [kaocha.classpath :as cp]
            [clojure.edn :as edn]))

(defn list-plugins
  "Produce a list of plugin jars"
  [dir]
  (->> (filter #(= (fs/extension %) "edn")
               (map str (babashka.fs/list-dir dir)))
       (map #(edn/read-string (slurp %)))
       (reduce
        (fn [result item]
          (let [key (:name item)]
            (assoc result key (dissoc item :name))))
        {})))

(defn load-plugin
  [dir plugin]
  (cp/add-classpath (str dir "/" (:jar plugin)))
  (require [(symbol (:main plugin))])
  {:create (resolve (symbol (:main plugin) "create"))
   :ip (resolve (symbol (:main plugin) "ip"))
   :provision (resolve (symbol (:main plugin) "provision"))
   :destroy (resolve (symbol (:main plugin) "destroy"))})
