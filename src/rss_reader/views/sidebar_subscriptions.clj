(ns rss-reader.views.sidebar-subscriptions
  (:import
   java.util.UUID)
  (:require
   [rss-reader.model :as model]
   [rss-reader.html :as html]))


(defn view
  ([user-id]
   (let [subscriptions
         (model/subscriptions-to-render user-id)]
     (view user-id subscriptions)))

  ([user-id subscriptions]
   )
  )
