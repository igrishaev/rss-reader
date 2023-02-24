(ns rss-reader.htmx.message-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.model :as model]))


(defn handler [{:keys [message-id]}]
  (let [
        ;; subscription
        ;; (model/get-subscription-by-id subscription-id)

        ;; messages
        ;; (model/messages-to-render subscription-id)

        ;; {:keys [opt_title
        ;;         rss_title]}
        ;; subscription

        ]

    [:div (format "Message %s" message-id)]

    #_
    [:div#feed-messages-list

     [:h1 (or opt_title rss_title)]

     [:div.message-brief
      [:div.message-date
       "Last updated at 15:01"]]

     [:div#feed-messages-table
      (for [message messages
            :let [{:keys [id
                          title
                          summary]} message]]

        [:div.feed-messages-row
         {:hx-post (html/api-url :viewMessage
                                 {:message-id 1})
          :hx-trigger "click"
          :hx-target "#content-inner"
          :hx-swap "innerHTML"}

         [:div.feed-messages-row-date
          "23 Feb"]
         [:div.feed-messages-row-content
          [:div.feed-messages-row-title title]
          [:div.feed-messages-row-teaser summary]]])]]))
