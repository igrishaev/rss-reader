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

        {:keys [feed_id
                opt_title
                rss_title
                sync_date_prev]}
        subscription

        feed
        (model/get-feed-by-id feed_id)

        {:keys [url_website]}
        feed

        title
        (or opt_title rss_title)]

    (html/response
     [:div#feed-messages-list

      (if url_website
        [:h1 [:a {:href url_website} title]]
        [:h1 title])

      [:div.message-brief
       [:div.message-date
        (str sync_date_prev)]]

      [:div#feed-messages-table
       (for [message messages

             :let [{:keys [id
                           entry]}
                   message

                   {:keys [title
                           link
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
           {:title (str date_published_at)}
           (html/ago date_published_at)]

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
         "more"])])))
