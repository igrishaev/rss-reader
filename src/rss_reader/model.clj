(ns rss-reader.model
  (:import
   java.util.UUID)
  (:require
   [rss-reader.db :as db]))


;;
;; Feed
;;

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
  ([url]
   (upsert-feed url nil))

  ([^String url fields]
   (let [row
         (-> fields
             (assoc :url_source url
                    :updated_at :%now))]
     (db/execute-one {:insert-into [:feeds]
                      :values [row]
                      :on-conflict [:url_source]
                      :do-update-set (keys row)
                      :returning [:*]}))))


(defn get-feed-by-id [^UUID feed-id]
  (db/execute-one {:select [:*]
                   :from [:feeds]
                   :where [:= :id feed-id]}
                  {:limit 1}))


(defn get-feed-by-url [^String url]
  (db/execute-one {:select [:*]
                   :from [:feeds]
                   :where [:= :url_source url]}
                  {:limit 1}))


;;
;; Entry
;;

(defn upsert-entry
  [^UUID feed-id ^String guid fields]
  (let [row
        (-> fields
            (assoc :feed_id feed-id
                   :guid guid
                   :updated_at :%now))]
    (db/execute-one {:insert-into [:entries]
                     :values [row]
                     :on-conflict [:feed_id :guid]
                     :do-update-set (keys row)
                     :returning [:*]})))


;;
;; Categories
;;

(defn upsert-categories
  [^UUID parent-id
   ^String parent-type
   categories]
  (when (seq categories)
    (db/execute {:insert-into [:categories]
                 :values (for [category categories]
                           {:parent_id parent-id
                            :parent-type parent-type
                            :category category})
                 :on-conflict [:parent_id :category]
                 :do-nothing true
                 :returning [:*]})))


#_
(comment

  (def -url "https://habr.com/ru/rss/all/all/?fl=ru")
  (def -user-id (random-uuid))

  (upsert-feed -url)

  (get-feed-by-url -url)

  (def -parent-id (random-uuid))

  (upsert-categories -parent-id "entry"
                     ["foo"
                      "bar"
                      "baz"]))
