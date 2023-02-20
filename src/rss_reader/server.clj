(ns rss-reader.server
  (:require
   [clojure.tools.logging :as log]
   [mount.core :as mount]
   [org.httpkit.server :as server]
   [rss-reader.config :refer [config]]
   [rss-reader.handler :as handler]))


(def defaults
  {:legacy-return-value? false})


(mount/defstate ^{:on-reload :noop} server
  :start
  (server/run-server handler/handler
                     (-> config
                         :http-server
                         (merge defaults)))

  :stop
  (server/server-stop! server))


(defn start []
  (mount/start (var server)))


(defn stop []
  (mount/stop (var server)))
