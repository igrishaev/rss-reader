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
   (:conn-manager config))

  :stop
  (conn-mgr/shutdown-manager cm))


(defn get
  ([url]
   (get url nil))

  ([url options]
   (client/get url
               (assoc options
                      :connection-manager cm))))
