(ns rss-reader.html
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))


(def API
  {"viewMessage"
   {:doc "test test"
    :spec :foo
    :auth? true
    :func (fn [])}

   "deleteSubscription"
   {:doc "test test"
    :spec ::foo
    :auth? true
    :func (fn [])}})


(defn handler [request]

  (let [{:keys [params]}
        request

        {:keys [action]}
        params]

    (if-let [{:keys [spec auth? func]}
             (get API action)]

      (let [params-ok
            (s/conform spec params)]

        (func params-ok))

      {:status 400
       :headers {"content-type" "text/html"}
       :body "wrong api"})))
