(ns rss-reader.model
  (:import
   java.util.UUID)
  (:require
   [rss-reader.const :as c]
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
                     :returning [:*]})))


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
                 :returning [:id :guid]})))


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


(defn get-subscription-by-id
  [^UUID subscription-id]
  (db/execute-one
   {:select [:s.*
             :f.rss_title
             :f.url_source
             :f.url_website
             :f.rss_domain]
    :from [[:subscriptions :s]
           [:feeds :f]]
    :where
    [:and
     [:= :s.id subscription-id]
     [:= :s.feed_id :f.id]]}))


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


(defn upsert-categories-bulk
  [rows]
  (when (seq rows)
    (db/execute {:insert-into [:categories]
                 :values rows
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
                   :limit c/max-messages-to-create}]
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
    :limit c/feeds-to-update-limit}))


(defn subscriptions-to-update []
  (db/execute
   {:select [:id :feed_id]
    :from [:subscriptions]
    :where [:or
            [:= :sync_date_next nil]
            [:< :sync_date_next :%now]]
    :order-by [[:sync_date_next :asc :nulls-first]]
    :limit c/subscriptions-to-update-limit}))


;;
;; Render
;;

(defn subscriptions-to-render []
  (db/execute
   {:select [:s.*
             :f.rss_title
             :f.url_source
             :f.url_website
             :f.rss_domain]
    :from [[:subscriptions :s]
           [:feeds :f]]
    :where [:= :s.feed_id :f.id]}))


(defn messages-to-render
  [^UUID subscription-id]
  (db/execute
   {:select [:e.feed_id
             :e.guid
             :e.link
             :e.author
             :e.title
             :e.teaser
             :e.date_published_at
             :e.date_updated_at
             :m.id
             :m.is_read
             :m.is_marked]
    :from [[:messages :m]
           [:entries :e]]
    :where
    [:and
     [:= :m.subscription_id subscription-id]
     [:= :m.entry_id :e.id]]}))


(defn message-to-render
  [^UUID message-id]
  (db/execute-one
   {:select [:e.feed_id
             :e.guid
             :e.link
             :e.author
             :e.title
             :e.summary
             :e.date_published_at
             :e.date_updated_at
             :m.id
             :m.is_read
             :m.is_marked]
    :from [[:messages :m]
           [:entries :e]]
    :where
    [:and
     [:= :m.id message-id]
     [:= :m.entry_id :e.id]]}))
