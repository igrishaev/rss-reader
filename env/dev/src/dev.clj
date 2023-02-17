(ns dev
  (:require
   [mount.core :as mount]
   rss-reader.config
   rss-reader.db))


(defn start []
  (mount/start (var rss-reader.config/config)
               (var rss-reader.db/db)))

(defn stop []
  (mount/stop))


#_
(comment

  (start)
  (stop)

  )
