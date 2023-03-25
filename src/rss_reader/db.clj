(ns rss-reader.db
  (:import
   java.util.UUID
   java.util.Date
   (java.sql PreparedStatement Timestamp))
  (:require
   [rss-reader.const :as c]
   [gosql.core :as gosql]
   [hikari-cp.core :as cp]
   [honey.sql :as honey]
   [mount.core :as mount]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as jdbc.prepare]
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


(defn execute
  ([query-map]
   (execute query-map nil))

  ([query-map options]
   (jdbc/execute!
    db
    (honey/format query-map)
    (assoc options
           :builder-fn rs/as-unqualified-maps))))


(defn execute-one
  ([query-map]
   (execute-one query-map nil))

  ([query-map options]
   (jdbc/execute-one!
    db
    (honey/format query-map)
    (assoc options
           :builder-fn rs/as-unqualified-maps))))


(defmacro with-tx [[opt] & body]
  `(jdbc/with-transaction [tx# db ~opt]
     (binding [db tx#]
       ~@body)))


(extend-protocol jdbc.prepare/SettableParameter
  Date
  (set-parameter [^Date v ^PreparedStatement s ^long i]
    (.setTimestamp s i (new Timestamp (.getTime v)))))


(gosql/from-resource "queries.sql"
                     {:db (var db)
                      :builder-fn rs/as-unqualified-maps})


(defn sync-subsciption
  [^UUID subscription-id ^UUID feed-id]
  (with-tx nil

    (create-messages-for-subscription
     {:subscription-id subscription-id
      :feed-id feed-id
      :limit c/max-messages-to-create})

    (update-unread-for-subscription
     {:subscription-id subscription-id})

    (update-sync-next-for-subscription
     {:subscription-id subscription-id})))
