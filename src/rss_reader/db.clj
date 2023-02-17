(ns rss-reader.db
  (:require
   [hikari-cp.core :as cp]
   [mount.core :as mount]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as jdbc.conn]
   [next.jdbc.result-set :as rs]
   [next.jdbc.sql :as sql]
   [rss-reader.config :refer [config]]))


(mount/defstate ^{:on-reload :noop} db
  :start
  (-> config
      :db-pool
      cp/make-datasource)

  :stop
  (cp/close-datasource db))


(defn start []
  (mount/start (var db)))


(defn stop []
  (mount/stop (var db)))


#_
(jdbc/execute! db ["select 1"])
