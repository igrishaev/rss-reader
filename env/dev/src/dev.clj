(ns dev
  (:require
   [mount.core :as mount]
   [rss-reader.config :as config]
   rss-reader.server
   rss-reader.db
   rss-reader.http))


(defn start []
  (config/with-profile :dev
    (mount/start (var config/config)
                 (var rss-reader.server/server)
                 (var rss-reader.db/db)
                 (var rss-reader.http/cm))))

(defn stop []
  (mount/stop))
