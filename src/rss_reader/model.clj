(ns rss-reader.model
  (:import
   java.util.UUID)
  (:require
   [rss-reader.db :as db]))


(defn create-feed
  [^String url ^UUID user-id]
  (db/execute-one {:insert-into [:feeds]
                   :values [{:url_source url
                             :user_id user-id}]
                   :returning [:*]}))


(defn get-feed-by-id [^UUID feed-id]
  (db/execute-one {:select [:*]
                   :from [:feeds]
                   :where [:= :id feed-id]}
                  {:limit 1}))
