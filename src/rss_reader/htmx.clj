(ns rss-reader.htmx
  (:require
   [clojure.spec.alpha :as s]
   rss-reader.htmx.auth
   rss-reader.htmx.message-view
   rss-reader.htmx.subscription-view
   [rss-reader.spec :as spec]))


(def API
  {

   "sendAuthEmail"
   {:doc "AAA"
    :spec ::spec/api-form-auth
    :auth? false
    :handler #'rss-reader.htmx.auth/handler}


   "viewSubscription"
   {:doc "aaa"
    :spec ::spec/api-view-subscription
    :auth? true
    :handler #'rss-reader.htmx.subscription-view/handler}

   "viewMessage"
   {:doc "test test"
    :spec ::spec/api-view-message
    :auth? true
    :handler #'rss-reader.htmx.message-view/handler}

   "deleteSubscription"
   {:doc "test test"
    :spec ::spec/api-delete-subscription
    :auth? true
    :handler (fn [])}})


(defn handler [request]

  (let [{:keys [params]}
        request

        {:keys [action]}
        params]

    (if-let [{:keys [spec auth? handler]}
             (get API action)]

      (let [params-ok
            (s/conform spec params)]

        (if (s/invalid? params-ok)

          {:status 400
           :headers {"content-type" "text/html"}
           :body "wrong params"}

          (handler params-ok)))

      {:status 400
       :headers {"content-type" "text/html"}
       :body "wrong api"})))
