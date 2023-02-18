(ns rss-reader.db
  (:require
   [hikari-cp.core :as cp]
   [honey.sql :as honey]
   [mount.core :as mount]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as jdbc.conn]
   [next.jdbc.result-set :as rs]
   [next.jdbc.sql :as sql]
   [rss-reader.config :refer [config]]))


(mount/defstate ^{:dynamic true :on-reload :noop} db
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


(defn execute [query-map]
  (jdbc/execute! db
                 (honey/format query-map)
                 {:builder-fn rs/as-unqualified-maps}))


(defn execute-one [query-map]
  (jdbc/execute-one! db
                     (honey/format query-map)
                     {:builder-fn rs/as-unqualified-maps}))


(defmacro with-tx [[opt] & body]
  `(jdbc/with-transaction [tx# db ~opt]
     (binding [db tx#]
       ~@body)))
