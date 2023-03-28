(ns rss-reader.htmx.message-view
  (:require
   [clojure.spec.alpha :as s]
   [rss-reader.spec :as spec]
   [rss-reader.html :as html]
   [rss-reader.views :as views]
   [rss-reader.db :as db]
   [rss-reader.model :as model]))


;; TODO: check if invalid
(defn handler [params]

  (let [params-ok
        (s/conform ::spec/api-view-message params)

        {:keys [message-id]}
        params-ok

        message
        (db/message-to-render {:message-id message-id})

        {:keys [entry_id
                subscription_id]}
        message

        categories
        (db/get-categories-by-parent-id {:entry-id entry_id})]

    (db/mark-message-read
     {:message-id message-id :flag true})

    (db/dec-subscription-read-counter
     {:subscription-id subscription_id})

    (let [subscription
          (db/get-subscription-by-id {:id subscription_id})]

      (html/response
       (views/message-view message categories)
       (views/subscription-row subscription)))))
