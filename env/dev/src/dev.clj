(ns dev
  (:require
   [mount.core :as mount]
   [rss-reader.config :as config]
   [rss-reader.cron :as cron]
   rss-reader.db
   rss-reader.http
   [rss-reader.model :as model]
   rss-reader.server))


(defn start []
  (config/with-profile :dev
    (mount/start (var config/config)
                 (var rss-reader.server/server)
                 (var rss-reader.db/db)
                 (var rss-reader.http/cm))))

(defn stop []
  (mount/stop))


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

    (cron/task-sync-feeds)
    (cron/task-sync-subscriptions)))
