(ns rss-reader.html
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
