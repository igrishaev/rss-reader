(ns rss-reader.htmx.message-view
  (:require
   [rss-reader.html :as html]
   [rss-reader.model :as model]))


(defn handler [{:keys [message-id]}]
  (let [message
        (model/message-to-render message-id)

        {:keys [guid
                link
                author
                title
                summary
                date_published_at
                date_updated_at
                id
                is_read
                is_marked]}
        message]

    [:div#message-content
     [:h1 title]

     [:div.message-brief

      (when author
        [:div.message-author
         author])

      [:div.message-date
       (str date_published_at)]]

     [:div.message-brief
      (for [tag ["aaa" "bbb" "ccc"]]
        [:div.message-tag tag])]

     [:div#message-summary
      summary]

     [:div#message-buttons
      [:div.button-normal
       "Read more"]]]))
