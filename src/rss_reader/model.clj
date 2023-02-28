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


(defn get-categories-by-parent-id
  [^UUID entry-id]
  (db/execute
   {:select [:*]
    :from [:categories]
    :where [:= :parent_id entry-id]}))


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
   {:insert-into [[:messages [:entry_id
                              :subscription_id
                              :date_published_at]]
                  {:select [:e.id
                            subscription-id
                            :e.date_published_at]
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
    :set {:sync_date_prev :%now
          :sync_date_next [:raw "now() + (interval '1 second' * sync_interval)"]
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


(def sql-cursor
  [:raw "(extract(epoch from date_published_at)::text || '|' || id)"])


(defn butlast-vec
  [^clojure.lang.PersistentVector vctr]
  (let [len (count vctr)]
    (if (pos? len)
      (subvec vctr 0 (dec len))
      vctr)))


(defn messages-to-render

  ([subscription-id]
   (messages-to-render nil))

  ([^UUID subscription-id {:keys [asc?
                                  cursor
                                  limit]
                           :or {asc? false
                                limit c/message-page-size}}]

   (let [direction
         (if asc? :asc :desc)

         comparator
         (if asc? :> :<)

         sql-messages
         (cond-> {:select [:id
                           :entry_id
                           :is_read
                           :is_marked
                           [sql-cursor :cursor]]
                  :from [:messages]
                  :order-by [[sql-cursor direction]]
                  :where
                  [:and [:= :subscription_id subscription-id]]
                  :limit (inc limit)}

           cursor
           (update :where conj [comparator sql-cursor cursor]))

         messages
         (db/execute sql-messages)

         more?
         (> (count messages) limit)

         messages
         (butlast-vec messages)

         cursor
         (-> messages peek :cursor)

         entry-ids
         (map :entry_id messages)

         entries
         (when (seq entry-ids)
           (db/execute {:select [:id
                                 :title
                                 :teaser
                                 :date_published_at]
                        :from [:entries]
                        :where [:in :id entry-ids]}))

         id->entry
         (reduce
          (fn [acc entry]
            (assoc acc (:id entry) entry))
          {}
          entries)]

     {:more? more?
      :cursor cursor
      :messages
      (reduce
       (fn [acc message]
         (let [{:keys [entry_id]}
               message

               entry
               (get id->entry entry_id)]

           (conj acc (assoc message :entry entry))))
       []
       messages)})))


(defn message-to-render
  [^UUID message-id]
  (db/execute-one
   {:select [[:e.id :entry_id]
             :e.feed_id
             :e.guid
             :e.link
             :e.author
             :e.title
             :e.summary
             :e.date_published_at
             :e.date_updated_at
             :m.id
             :m.subscription_id
             :m.is_read
             :m.is_marked]
    :from [[:messages :m]
           [:entries :e]]
    :where
    [:and
     [:= :m.id message-id]
     [:= :m.entry_id :e.id]]}))
