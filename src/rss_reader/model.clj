(ns rss-reader.model
  (:import
   java.util.UUID)
  (:require
   [rss-reader.db :as db]))


(defn update-feed
  [^UUID feed-id fields]
  (let [row
        (-> fields
            (assoc :updated_at :%now))]
    (db/execute-one {:update [:feeds]
                     :set row
                     :where [:= :id feed-id]
                     :returning [:*]})))


(defn upsert-feed
  ([url user-id]
   (upsert-feed url user-id nil))

  ([^String url ^UUID user-id fields]
   (let [row
         (-> fields
             (assoc :url_source url
                    :user_id user-id
                    :updated_at :%now))]
     (db/execute-one {:insert-into [:feeds]
                      :values [row]
                      :on-conflict [:user_id :url_source]
                      :do-update-set (keys row)
                      :returning [:*]}))))


(defn get-feed-by-id [^UUID feed-id]
  (db/execute-one {:select [:*]
                   :from [:feeds]
                   :where [:= :id feed-id]}
                  {:limit 1}))


(defn get-feed-by-url [^String url ^UUID user-id]
  (db/execute-one {:select [:*]
                   :from [:feeds]
                   :where [:and
                           [:= :user_id user-id]
                           [:= :url_source url]]}
                  {:limit 1}))


#_
(comment

  (def -url "https://habr.com/ru/rss/all/all/?fl=ru")
  (def -user-id (random-uuid))

  (upsert-feed -url -user-id)

  (get-feed-by-url -url -user-id)

  )
