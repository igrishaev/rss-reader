(ns rss-reader.handlers.index
  (:require
   [rss-reader.html :as html]
   [rss-reader.views.index :as index]))


(defn handler [request]
  (html/response
   (index/view nil)))
