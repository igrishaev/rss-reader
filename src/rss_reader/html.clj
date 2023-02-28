(ns rss-reader.html
  (:import java.util.Date)
  (:require
   [ring.util.codec :as codec]
   [hiccup.core :as hiccup]))


(defn api-url
  [action params]
  (format "/htmx?%s"
          (codec/form-encode
           (assoc params :action (name action)))))


(defn response [struct]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (hiccup/html struct)})


(def rules
  [[(* 1000 60 60 24 30 12) "y"]
   [(* 1000 60 60 24 30) "m"]
   [(* 1000 60 60 24 7) "w"]
   [(* 1000 60 60 24) "d"]
   [(* 1000 60 60) "h"]])


(defn get-diff [^Date d]
  (abs (- (.getTime (new Date))
          (.getTime d))))


(defn ago [^Date d]
  (let [diff
        (get-diff d)]
    (loop [[[n t :as rule] & rules] rules]
      (if rule
        (let [x (quot diff n)]
          (if (pos? x)
            (format "%d%s" x t)
            (recur rules)))
        "now"))))
