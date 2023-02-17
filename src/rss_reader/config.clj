(ns rss-reader.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [mount.core :as mount]))


(def ^:dynamic *profile* nil)


(defmacro with-profile [profile & body]
  `(binding [*profile* ~profile]
     ~@body))


(mount/defstate ^{:on-reload :noop} config
  :start
  (do
    (log/infof "Loading the config, profile: %s" *profile*)
    (-> "config.edn"
        (io/resource)
        (or (throw (ex-info "Config file not found" {})))
        (aero/read-config {:profile *profile*}))))


(defn start []
  (mount/start (var config)))


(defn stop []
  (mount/stop (var config)))
