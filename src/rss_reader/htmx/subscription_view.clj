(ns rss-reader.htmx.subscription-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.model :as model]))


(defn handler [{:keys [subscription-id
                       cursor]}]
  (let [subscription
        (model/get-subscription-by-id subscription-id)

        messages
        (model/messages-to-render subscription-id false cursor)

        cursor
        (-> messages peek :cursor)

        {:keys [opt_title
                rss_title]}
        subscription]

    [:div#feed-messages-list

     [:h1 (or opt_title rss_title)]

     [:div.message-brief
      [:div.message-date
       "Last updated at 15:01"]]

     [:div#feed-messages-table
      (for [message messages
            :let [{:keys [id
                          title
                          teaser]} message]]

        [:div.feed-messages-row
         {:hx-post (html/api-url :viewMessage
                                 {:message-id id})
          :hx-trigger "click"
          :hx-target "#content-inner"
          :hx-swap "innerHTML"}

         [:div.feed-messages-row-date
          "23 Feb"]
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
