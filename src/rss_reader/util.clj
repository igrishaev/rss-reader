(ns rss-reader.util
  (:require
   [clojure.string :as str]))


(defn by-chunks [coll n]
  (partition n n [] coll))


(defn get-charset
  ^String [^String content-type]
  (some-> #"(?i)charset\s*=\s*(.+)"
          (re-find content-type)
          (second)
          (str/trim)
          (not-empty)))
