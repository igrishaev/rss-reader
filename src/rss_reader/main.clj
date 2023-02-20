(ns rss-reader.main
  (:gen-class)
  (:require
   [mount.core :as mount]
   [signal.handler :as signal]
   [clojure.tools.logging :as log]
   rss-reader.config
   rss-reader.db
   rss-reader.cron
   rss-reader.server))


(defn -main
  [& _]

  (log/info "Starting the system...")

  (mount/start (var rss-reader.config/config)
               (var rss-reader.db/db)
               (var rss-reader.cron/cron)
               (var rss-reader.server/server))

  (log/info "The system has been started.")

  (signal/with-handler :term
    (log/info "Caught SIGTERM, stopping the system...")
    (mount/stop)
    (log/info "The system has been stopped.")
    (System/exit 0)))
