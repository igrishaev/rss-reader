(ns rss-reader.handlers
  (:require
   [rss-reader.html :as html]
   [rss-reader.views :as views]))


(defn index [request]
  (html/response
   (views/index nil)))
