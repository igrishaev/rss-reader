(ns dev
  (:refer-clojure :exclude [sync])
  (:require
   [mount.core :as mount]
   [rss-reader.config :as config]
   [rss-reader.cron :as cron]
   [rss-reader.db :as db]
   rss-reader.http
   rss-reader.log
   [rss-reader.model :as model]
   rss-reader.server))


(defn start
  ([]
   (start :dev))

  ([profile]
   (config/with-profile profile
     (mount/start (var config/config)
                  (var rss-reader.server/server)
                  (var rss-reader.log/log)
                  (var rss-reader.db/db)
                  (var rss-reader.http/cm)))))

(defn stop []
  (mount/stop))


(defn sync []
  (cron/task-sync-feeds)
  (cron/task-sync-subscriptions))


(defn reset []
  (db/reset-subscriptions-sync)
  (db/reset-feeds-sync))


(defn seed []

  (let [email1
        "test1@test.com"

        {auth-id :id}
        (db/add-auth-code {:email email1})

        {user-id-1 :id}
        (db/upsert-user {:email "test1@test.com"})

        {user-id-2 :id}
        (db/upsert-user {:email "test1@test.com"})

        {feed-id-1 :id}
        (db/upsert-feed {:fields {:url_source "https://habr.com/ru/rss/all/all/?fl=ru"}})

        {feed-id-2 :id}
        (db/upsert-feed {:fields {:url_source "http://oglaf.com/feeds/rss/"}})

        {feed-id-3 :id}
        (db/upsert-feed {:fields {:url_source "https://ilyabirman.ru/meanwhile/rss/"}})

        {feed-id-4 :id}
        (db/upsert-feed {:fields {:url_source "https://grishaev.me/feed.xml"}})

        {sub-id-1 :id}
        (db/upsert-subscription {:fields {:feed_id feed-id-1 :user_id user-id-1}})

        {sub-id-2 :id}
        (db/upsert-subscription {:fields {:feed_id feed-id-2 :user_id user-id-1}})

        {sub-id-3 :id}
        (db/upsert-subscription {:fields {:feed_id feed-id-3 :user_id user-id-2}})

        {sub-id-4 :id}
        (db/upsert-subscription {:fields {:feed_id feed-id-4 :user_id user-id-2}})]

    auth-id

    #_
    (sync))
  )


(defn unread []
  (db/set-messages-unread)
  )

#_
(defn bootstrap []

  (let [{user-id-1 :id}
        (model/upsert-user "test1@test.com")

        {user-id-2 :id}
        (model/upsert-user "test1@test.com")

        {feed-id-1 :id}
        (model/upsert-feed "https://habr.com/ru/rss/all/all/?fl=ru")

        {feed-id-2 :id}
        (model/upsert-feed "http://oglaf.com/feeds/rss/")

        {feed-id-3 :id}
        (model/upsert-feed "https://ilyabirman.ru/meanwhile/rss/")

        {feed-id-4 :id}
        (model/upsert-feed "https://grishaev.me/feed.xml")

        {sub-id-1 :id}
        (model/upsert-subscription feed-id-1 user-id-1)

        {sub-id-2 :id}
        (model/upsert-subscription feed-id-2 user-id-1)

        {sub-id-3 :id}
        (model/upsert-subscription feed-id-3 user-id-2)

        {sub-id-4 :id}
        (model/upsert-subscription feed-id-4 user-id-2)]

    (sync)))
