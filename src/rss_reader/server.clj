(ns rss-reader.server
  (:require
   [clojure.tools.logging :as log]
   [mount.core :as mount]
   [org.httpkit.server :as server]
   [rss-reader.config :refer [config]]
   [rss-reader.handler :as handler]))


(mount/defstate ^{:on-reload :noop} server
  :start
  (server/run-server handler/app
                     {:max-line 8192
                      :max-body 100663296
                      :port 18088
                      :legacy-return-value? false})

  :stop
  (server/server-stop! server))


(defn start []
  (mount/start (var server)))


(defn stop []
  (mount/stop (var server)))
