(ns rss-reader.handlers.index
  (:require
   [hiccup.core :as hiccup]))


(defn html-response [struct]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (hiccup/html struct)})


(defn handler [request]
  (html-response
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

         [:div.feed-item
          {:hx-get "/foo/bar"
           :hx-trigger "click"
           :hx-target "#content-inner"
           :hx-swap "innerHTML"}
          [:div.feed-title
           "Ivan Grishaev is writing on diffent stuff foo bar baz"]
          [:div.feed-unread
           128]]

         [:div.feed-item
          [:div.feed-title
           "Лучшее за сутки"]
          [:div.feed-unread
           32]]

         [:div.feed-item.feed-item-active
          [:div.feed-title
           "Блог Васи Пупкина"]
          [:div.feed-unread
           3]]]]]

      [:div#content
       [:div#content-inner
        [:div#content-actions
         ]
        ]
       ]

      ]]]))


(defn dialog [request]
  (html-response
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


(defn subscription-view [request]
  )


(defn subscription-delete-dialog [request]
  )



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


(defn http-subscription-delete [request]
  (html-response
   (struct-subscription-delete (random-uuid))))


(defn message-view [request]
  )


(defn message-mark-read [request]
  )


(defn message-mark-unread [request]
  )


(defn message-mark-star [request]
  )


(defn message-mark-unstar [request]
  )


(defn today-view [request]
  )
