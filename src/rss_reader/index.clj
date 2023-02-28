(ns rss-reader.index
  (:require
   [rss-reader.model :as model]
   [rss-reader.html :as html]))


(defn handler [request]

  (let [subscriptions
        (model/subscriptions-to-render)]

    (html/response
     [:html {:lang "en"}
      [:head
       [:meta {:charset "utf-8"}]
       [:title "Hello"]
       [:link {:href "/css/style.css" :rel "stylesheet"}]
       [:script {:src "https://unpkg.com/htmx.org@1.8.5"
                 :crossorigin "anonymous"}]]
      [:body
       [:div#container

        [:div#sidebar

         [:div#sidebar-top-section
          [:div#sidebar-logo-and-title
           [:div#sidebar-logo
            [:img {:src "/img/logo.svg"}]]
           [:div#sidebar-logo-text
            [:div#sidebar-logo-title
             "RSS Simple (dot) DEV"]
            [:div#sidebar-logo-slogan
             "An open-source RSS reader written in Clojure & HTMX"]]]
          [:div#sidebar-main-menu]]

         [:div#sidebar-inner

          [:div#sidebar-menu
           [:div.sidebar-menu-item
            "Today"]
           [:div.sidebar-menu-item
            "Starred messages"]
           [:div.sidebar-menu-item
            "Add feed"]]

          [:div#feed-list

           [:div.feed-list-caption
            "Unread"]

           (for [subscription subscriptions
                 :let [{:keys [id
                               rss_title
                               opt_title
                               unread_count]}
                       subscription]
                 ]
             [:div.feed-item
              {:hx-post (html/api-url :viewSubscription
                                      {:subscription-id id})
               :hx-trigger "click"
               :hx-target "#content-inner"
               :hx-swap "innerHTML"}
              [:div.feed-title
               (or opt_title rss_title)]
              [:div.feed-unread
               unread_count]])

           #_
           [:div.feed-item
            {:hx-get "/foo/bar"
             :hx-trigger "click"
             :hx-target "#content-inner"
             :hx-swap "innerHTML"}
            [:div.feed-title
             "Ivan Grishaev is writing on diffent stuff foo bar baz"]
            [:div.feed-unread
             128]]

           #_
           [:div.feed-item.feed-item-active
            [:div.feed-title
             "Блог Васи Пупкина"]
            [:div.feed-unread
             3]]]]]

        [:div#content
         [:div#content-inner]
         ]

        ]]])))


(defn dialog [request]
  (html/response
   [:div.dialog
    [:div.dialog-title
     "Лучшее за сутки / habrahabr.com"]
    [:div.dialog-subtitle
     "Are you sure you want to delete that feed? You can subscribe to it any time you want later."]
    [:div.dialog-buttons
     [:div.dialog-button-normal
      "No"]
     [:div.dialog-button-normal.dialog-button-dangerous
      "Yes"]]]))


(defn struct-subscription-delete [subscription-id]
  [:div.dialog
   [:div.dialog-title
    "Лучшее за сутки / habrahabr.com"]
   [:div.dialog-subtitle
    "Are you sure you want to delete that feed? You can subscribe to it any time you want later."]
   [:div.dialog-buttons
    [:div.dialog-button-normal
     "No"]
    [:div.dialog-button-normal.dialog-button-dangerous
     "Yes"]]])
