(ns rss-reader.htmx.message-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.model :as model]))


(defn handler [{:keys [message-id]}]
  (let [message
        (model/message-to-render message-id)

        {:keys [entry_id
                guid
                link
                author
                title
                summary
                date_published_at
                date_updated_at
                id
                subscription_id
                is_read
                is_marked]}
        message

        categories
        (model/get-categories-by-parent-id entry_id)]

    (model/mark-message-read message-id true)
    (model/dec-subscription-read-counter subscription_id)

    [:div

     [:div#content-actions
      [:div.content-action
       {:hx-post (html/api-url :viewSubscription
                               {:subscription-id subscription_id})
        :hx-trigger "click"
        :hx-target "#content-inner"
        :hx-swap "innerHTML"}
       "&larr; Back"]]

     [:div#message-content

      (if link
        [:h1 [:a {:href link} title]]
        [:h1 title])

      [:div.message-brief

       (when author
         [:div.message-author
          "Author: " author])

       [:div.message-date
        {:title (str date_published_at)}
        "Publised: "
        (html/ago date_published_at)]]

      [:div.message-brief
       (for [{:keys [category]} categories]
         [:div.message-tag category])]

      [:div#message-summary
       summary]

      [:div#message-buttons
       (when link
         [:div.button-normal
          [:a {:href link}
           "Read more"]])]]]))
