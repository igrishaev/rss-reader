(ns rss-reader.htmx.message-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.views :as views]
   [rss-reader.model :as model]))


(defn handler [{:keys [message-id]}]
  (let [message
        (model/message-to-render message-id)

        {:keys [entry_id
                subscription_id]}
        message

        categories
        (model/get-categories-by-parent-id entry_id)]

    (model/mark-message-read message-id true)
    (model/dec-subscription-read-counter subscription_id)

    (let [subscription
          (model/get-subscription-by-id subscription_id)]

      (html/response
       (views/message-view message categories)
       (views/subscription-row subscription)))))
