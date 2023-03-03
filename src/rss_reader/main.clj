(ns rss-reader.main
  (:gen-class)
  (:require
   [clojure.tools.logging :as log]
   [mount.core :as mount]
   rss-reader.config
   rss-reader.cron
   rss-reader.db
   rss-reader.log
   rss-reader.server
   [signal.handler :as signal]))


(defn exit [code]
  (System/exit code))


(defn start []
  (mount/start (var rss-reader.config/config)
               (var rss-reader.db/db)
               (var rss-reader.log/log)
               (var rss-reader.http/cm)
               (var rss-reader.cron/cron)
               (var rss-reader.server/server)))


(defn stop []
  (mount/stop))


(defn -main
  [& _]

  (log/info "Starting the system...")

  (try
    (start)
    (catch Throwable e
      (log/errorf e "The system has failed to start")
      (exit 1)))

  (log/info "The system has been started.")

  (signal/with-handler :term
    (log/info "Caught SIGTERM, stopping the system...")
    (stop)
    (log/info "The system has been stopped.")
    (exit 0))

  (signal/with-handler :int
    (log/info "Caught SIGINT, stopping the system...")
    (stop)
    (log/info "The system has been stopped.")
    (exit 0))

  (signal/with-handler :hup
    (log/info "Caught SIGHUP, restarting the system...")
    (stop)
    (start)
    (log/info "The system has been restarted.")))
