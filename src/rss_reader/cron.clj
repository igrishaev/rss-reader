(ns rss-reader.cron
  (:import
   java.util.concurrent.TimeUnit
   java.util.concurrent.Future
   java.util.concurrent.ScheduledThreadPoolExecutor)
  (:require
   [rss-reader.const :as c]
   [rss-reader.model :as model]
   [rss-reader.feed :as feed]
   [rss-reader.db :as db]
   [mount.core :as mount]
   [clojure.tools.logging :as log]
   [rss-reader.config :refer [config]]))


(defn task-sync-subscriptions []
  (let [rows
        (db/subscriptions-to-update {:limit c/subscriptions-to-update-limit})]
    (log/infof "Got %s subsciption(s) to update" (count rows))
    (doseq [{:keys [id feed_id]} rows]
      (log/infof "Syncing subscription %s, feed %s" id feed_id)
      (model/sync-subsciption id feed_id))))


(defn task-sync-feeds []
  (let [rows
        (db/feeds-to-update {:limit c/feeds-to-update-limit})]
    (log/infof "Got %s feeds(s) to update" (count rows))
    (doseq [{:keys [id]} rows]
      (log/infof "Syncing feed %s" id)
      (feed/update-feed-safe id))))


(defn task-expire-auth-codes []
  (model/expire-auth-codes))


(def TASKS

  [{:delay (* 60 5)
    :period (* 60 60)
    :title "<Sync Feeds>"
    :func task-sync-feeds}

   {:delay (* 60 35)
    :period (* 60 60)
    :title "<Sync Subscriptions>"
    :func task-sync-subscriptions}

   {:delay (* 60 1)
    :period (* 60 10)
    :title "<Expire Auth Codes>"
    :func task-expire-auth-codes}])


(defn wrap-task [func title]
  (fn []
    (log/infof "Cron task %s has been started" title)
    (try
      (func)
      (catch Throwable e
        (log/errorf e "Cron task %s has failed" title))
      (finally
        (log/infof "Cron task %s has been stopped" title)))))


(mount/defstate ^{:on-reload :noop} cron
  :start
  (let [executor
        (new ScheduledThreadPoolExecutor 2)

        futures
        (vec (for [{:keys [delay period title func]}
                   TASKS]
               (.scheduleAtFixedRate executor
                                     (wrap-task func title)
                                     delay
                                     period
                                     TimeUnit/SECONDS)))]

    {:executor executor
     :futures futures})

  :stop
  (let [{:keys [executor futures]}
        cron]
    (doseq [^Future future futures]
      (.cancel future false))
    (.shutdown ^ScheduledThreadPoolExecutor executor)))


(defn start []
  (mount/start (var cron)))


(defn stop []
  (mount/stop (var cron)))
