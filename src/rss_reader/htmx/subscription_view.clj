(ns rss-reader.htmx.subscription-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.model :as model]))


(defn handler [{:keys [subscription-id
                       cursor]}]
  (let [subscription
        (model/get-subscription-by-id subscription-id)

        {:keys [more?
                cursor
                messages]}
        (model/messages-to-render subscription-id
                                  {:asc? false
                                   :cursor cursor})

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
      (for [message messages

            :let [{:keys [id
                          entry]}
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

     (when more?
       [:a
        {:hx-post (html/api-url :viewSubscription
                                {:subscription-id subscription-id
                                 :cursor cursor})
         :hx-trigger "click"
         :hx-target "#content-inner"
         :hx-swap "innerHTML"}
        "more"])]))
