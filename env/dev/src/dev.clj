(ns dev
  (:require
   [mount.core :as mount]
   [rss-reader.config :as config]
   rss-reader.db))


(defn start []
  (config/with-profile :dev
    (mount/start (var config/config)
                 (var rss-reader.db/db))))

(defn stop []
  (mount/stop))
