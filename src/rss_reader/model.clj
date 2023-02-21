(ns rss-reader.model
  (:import
   java.util.UUID)
  (:require
   [rss-reader.db :as db]))


(defn upsert-fields [row]
  (-> row (dissoc :id) keys))


;;
;; User
;;

(defn upsert-user
  ([^String email]
   (upsert-user email nil))

  ([^String email fields]
   (let [row
         (-> fields
             (assoc :email email
                    :updated_at :%now))]
     (db/execute-one {:insert-into [:users]
                      :values [row]
                      :on-conflict [:email]
                      :do-update-set (upsert-fields row)
                      :returning [:*]}))))


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
                      :do-update-set (upsert-fields row)
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
                     :do-update-set (upsert-fields row)
                     :returning [:id]})))


(defn upsert-entries
  [^UUID feed-id entry-rows]
  (let [rows
        (for [row entry-rows]
          (assoc row
                 :feed_id feed-id
                 :updated_at :%now))]
    (db/execute {:insert-into [:entries]
                 :values rows
                 :on-conflict [:feed_id :guid]
                 :do-update-set (upsert-fields (first rows))
                 :returning [:id]})))


;;
;; Subscription
;;


(defn upsert-subscription
  [^UUID feed-id ^UUID user-id]
  (let [row
        {:feed_id feed-id
         :user_id user-id
         :updated_at :%now}]
    (db/execute-one {:insert-into [:subscriptions]
                     :values [row]
                     :on-conflict [:feed_id :user_id]
                     :do-update-set (upsert-fields row)
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


;;
;; Sync
;;

(defn create-messages-for-subscription
  [^UUID subscription-id ^UUID feed-id]
  (db/execute
   {:insert-into [[:messages [:entry_id :subscription_id]]
                  {:select [:e.id subscription-id]
                   :from [[:entries :e]]
                   :where [:= :e.feed_id feed-id]
                   :order-by [[:created_at :desc]]
                   :limit 1000}]
    :on-conflict [:entry_id :subscription_id]
    :do-nothing true
    :returning [:*]}))


(defn update-unread-for-subscription
  [^UUID subscription-id]
  (db/execute
   {:update [:subscriptions]
    :set {:unread_count :sub.unread}
    :from [[{:select [[[:count :m.id] :unread]]
             :from [[:messages :m]]
             :where [:and
                     [:= :m.subscription_id subscription-id]
                     [:not :is_read]]}
            :sub]]
    :where [:= :id subscription-id]
    :returning [:*]}))


(defn update-sync-next-for-subscription
  [^UUID subscription-id]
  (db/execute
   {:update [:subscriptions]
    :set {:sync_date_next [:raw "now() + (interval '1 second' * sync_interval)"]
          :sync_count [:raw "sync_count + 1"]}
    :where [:= :id subscription-id]
    :returning [:*]}))


(defn sync-subsciption
  [^UUID subscription-id ^UUID feed-id]
  (db/with-tx nil
    (create-messages-for-subscription subscription-id feed-id)
    (update-unread-for-subscription subscription-id)
    (update-sync-next-for-subscription subscription-id)))


(defn feeds-to-update []
  (db/execute
   {:select [:id]
    :from [:feeds]
    :where [:or
            [:= :sync_date_next nil]
            [:< :sync_date_next :%now]]
    :order-by [[:sync_date_next :asc :nulls-first]]
    :limit 100}))


(defn subscriptions-to-update []
  (db/execute
   {:select [:id :feed_id]
    :from [:subscriptions]
    :where [:or
            [:= :sync_date_next nil]
            [:< :sync_date_next :%now]]
    :order-by [[:sync_date_next :asc :nulls-first]]
    :limit 100}))


#_
(comment

  (create-messages-for-subscription
   #uuid "42230fa5-c115-488d-a36e-af0d650ec771"
   #uuid "2e86f35b-569a-4220-a25b-a646233b1508")

  (def -url "https://habr.com/ru/rss/all/all/?fl=ru")
  (def -user-id (random-uuid))

  ;; http://oglaf.com/feeds/rss/

  (upsert-feed -url)

  (get-feed-by-url -url)

  (def -parent-id (random-uuid))

  (upsert-categories -parent-id "entry"
                     ["foo"
                      "bar"
                      "baz"]))
