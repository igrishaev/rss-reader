(ns rss-reader.http
  (:refer-clojure :exclude [get])
  (:require
   [clj-http.client :as client]
   [clj-http.conn-mgr :as conn-mgr]
   [mount.core :as mount]
   [rss-reader.config :refer [config]]))


(mount/defstate ^{:on-reload :noop} cm
  :start
  (conn-mgr/make-reusable-conn-manager
   (-> config :http :conn-manager))

  :stop
  (conn-mgr/shutdown-manager cm))


(defn start []
  (mount/start (var cm)))


(defn stop []
  (mount/stop (var cm)))


(defn get
  ([url]
   (get url nil))

  ([url options]
   (client/get url
               (-> (-> config :http :defaults)
                   (merge options)
                   (assoc :connection-manager cm)))))


(defn ok? [response]
  (-> response :status (= 200)))


(defn content-type? [response regex]
  (some-> response
          :headers
          (clojure.core/get "content-type")
          (->> (re-find regex))))


(defn html? [response]
  (content-type? response #"(?i)text/html"))


(defn feed? [response]
  (content-type? response #"(?i)rss|atom|xml"))
