(ns rss-reader.server
  (:require
   [clojure.tools.logging :as log]
   [mount.core :as mount]
   [org.httpkit.server :as server]
   [rss-reader.config :refer [config]]
   [rss-reader.app :as app]))


(def defaults
  {:legacy-return-value? false})


(mount/defstate ^{:on-reload :noop} server
  :start
  (server/run-server app/app
                     (-> config
                         :http-server
                         (merge defaults)))

  :stop
  (server/server-stop! server))


(defn start []
  (mount/start (var server)))


(defn stop []
  (mount/stop (var server)))
