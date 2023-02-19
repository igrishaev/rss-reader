(ns rss-reader.cron
  (:import
   java.util.concurrent.TimeUnit
   java.util.concurrent.Future
   java.util.concurrent.ScheduledThreadPoolExecutor)
  (:require
   [mount.core :as mount]
   [clojure.tools.logging :as log]
   [rss-reader.config :refer [config]]))


(defn wrap-func [func title]
  (fn []
    (log/infof "Cron task %s has been started" title)
    (try
      (func)
      (catch Throwable e
        (log/errorf e "Cron task %s has failed" title))
      (finally
        (log/infof "Cron task %s has been stopped" title)))))


(def TASKS
  [{:delay 15
    :period (* 1 60 60)
    :title "Print Hello"
    :func #(println "Hello")}
   {:delay 30
    :period (* 1 60 60)
    :title "Print World"
    :func #(println "World")}])


(mount/defstate ^{:on-reload :noop} cron
  :start
  (let [executor
        (new ScheduledThreadPoolExecutor 2)

        futures
        (vec (for [{:keys [delay period title func]}
                   TASKS]
               (.scheduleAtFixedRate executor
                                     delay
                                     period
                                     (wrap-func func title)
                                     TimeUnit/SECONDS)))]

    {:executor executor
     :futures futures})

  :stop
  (let [{:keys [executor futures]}
        cron]
    (doseq [^Future future futures]
      (.cancel future false))
    (.shutdown ^ScheduledThreadPoolExecutor executor)))
