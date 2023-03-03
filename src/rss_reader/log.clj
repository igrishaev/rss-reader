(ns rss-reader.log
  (:require
   [mount.core :as mount]
   [clojure.tools.logging :as log]))


(defn ex-chain [e]
  (loop [e e
         result []]
    (if (nil? e)
      result
      (recur (ex-cause e)
             (conj result e)))))


(defn ex-print
  [^Throwable e]
  (let [indent "  "]
    (doseq [e (ex-chain e)]
      (println (-> e
                   class
                   .getCanonicalName))
      (print indent)
      (println (ex-message e))
      (when-let [data (ex-data e)]
        (print indent)
        (clojure.pprint/pprint data)))))


(defn make-ex-logger
  [log*]
  (fn [logger lvl e msg]
    (if e
      (let [msg*
            (str msg \newline
                 (with-out-str
                   (ex-print e)))]
        (log* logger lvl nil msg*))
      (log* logger lvl e msg))))


(defn install-ex-logger []
  (alter-var-root #'log/log* make-ex-logger))


(mount/defstate ^{:on-reload :noop} log

  :start
  (let [log-old log/log*]
    (install-ex-logger)
    log-old)

  :stop
  (alter-var-root #'log/log*
                  (constantly log)))


(defn start []
  (mount/start (var log)))


(defn stop []
  (mount/stop (var log)))
