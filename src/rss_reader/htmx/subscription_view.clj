(ns rss-reader.htmx.subscription-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.model :as model]))


(defn handler [{:keys [subscription-id
                       cursor]}]
  (let [subscription
        (model/get-subscription-by-id subscription-id)

        pairs
        (model/messages-to-render subscription-id false cursor)

        cursor
        (-> pairs peek :message :cursor)

        {:keys [opt_title
                rss_title
                sync_date_prev]}
        subscription]

    [:div#feed-messages-list

     [:h1 (or opt_title rss_title)]

     [:div.message-brief
      [:div.message-date
       (str sync_date_prev)]]

     [:div#feed-messages-table
      (for [{:keys [message entry]} pairs
            :let [{:keys [id]}
                  message

                  {:keys [title
                          teaser
                          date_published_at]}
                  entry]]

        [:div.feed-messages-row
         {:hx-post (html/api-url :viewMessage
                                 {:message-id id})
          :hx-trigger "click"
          :hx-target "#content-inner"
          :hx-swap "innerHTML"}

         [:div.feed-messages-row-date
          (str date_published_at)]
         [:div.feed-messages-row-content
          [:div.feed-messages-row-title title]
          [:div.feed-messages-row-teaser teaser]]])]

     [:a
      {:hx-post (html/api-url :viewSubscription
                              {:subscription-id subscription-id
                               :cursor cursor})
       :hx-trigger "click"
       :hx-target "#content-inner"
       :hx-swap "innerHTML"}
      "more"]]))
