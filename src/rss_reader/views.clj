(ns rss-reader.views
  (:import
   java.util.UUID)
  (:require
   [rss-reader.db :as db]
   [rss-reader.model :as model]
   [rss-reader.html :as html]))


(defn sidebar-subscription [subscritpion]

  (let [{:keys [id
                opt_title
                rss_title
                url_source
                url_website
                unread_count]}
        subscritpion

        title
        (or opt_title rss_title url_website url_source)

        url
        (html/api-url :viewSubscription {:subscription-id id})]

    [:div
     {:id (format "uuid-%s-sidebar-item" id)
      :class "feed-item"
      :hx-swap-oob "true"
      :hx-post url
      :hx-trigger "click"
      :hx-target "#content-inner"
      :hx-swap "innerHTML"}

     [:div.feed-title title]
     [:div.feed-unread unread_count]]))


(defn sidebar-subscriptions
  ([user]
   (let [subscriptions
         (db/subscriptions-to-render {:user-id (:id user)})]
     (sidebar-subscriptions user subscriptions)))

  ([user subscriptions]

   [:div#feed-list

    [:div.feed-list-caption
     "Unread"]

    (for [subscription subscriptions
          :let [{:keys [id
                        rss_title
                        opt_title
                        unread_count]}
                subscription]]

      (sidebar-subscription subscription))]))


(defn message-full [^UUID message-id]
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

    (let [subscription
          (model/get-subscription-by-id subscription_id)]

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
             "Read more"]])]]])))



#_
(defn sidebar-subscription [subscritpion]

  (let [{:keys [id
                opt_title
                unread_count]}
        subscritpion

        url
        (html/api-url :viewSubscription {:subscription-id id})]

    [:div
     {:id (format "uuid-%s-sidebar-item" id)
      :class "feed-item"
      :hx-swap-oob "true"
      :hx-post url
      :hx-trigger "click"
      :hx-target "#content-inner"
      :hx-swap "innerHTML"}

     [:div.feed-title
      opt_title]
     [:div.feed-unread
      unread_count]]))


(defn form-auth
  ([]
   (form-auth nil))

  ([{:keys [email email-error]}]
   [:div
    [:h1 "Sign in by Email"]
    [:p "No password is required. We'll send you a magic link to authorize."]
    [:form
     {:hx-post "/htmx"
      :hx-target "#content-inner"
      :hx-swap "innerHTML"}
     [:div.form-field
      [:input.form-input
       {:type "email"
        :name "email"
        :placeholder "user@domain.com"
        :value email}]
      (when email-error
        [:div.form-error email-error])]
     [:div.dialog-buttons
      [:button.dialog-button-normal
       {:type :submit
        :name :action
        :value :sendAuthEmail}
       "Send the link"]]]]))


(defn form-auth-sent []
  [:div
   [:h1 "Thank you!"]
   [:p "Now check out your mailbox, please."]])


(defn user-dropdown [user]
  [:div#user-widget
   {:class "content-action dropdown"}
   [:span (:email user)]
   [:div.dropdown-content
    [:a {:href "#"} "Settings"]
    [:a {:href "#"} "Logout"]]])


(defn index

  ([user]
   (index user nil))

  ([user {:keys [message]}]

   (let []

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
            "Today2"]
           [:div.sidebar-menu-item
            "Starred messages"]
           [:div.sidebar-menu-item
            "Add feed"]]

          (when user
            (sidebar-subscriptions user))]]

        [:div#content

         (when message
           [:div#alert message])

         [:div#content-inner

          [:div#content-actions
           [:div.flex-separator]
           (when user
             (user-dropdown user))]

          (when-not user
            (form-auth))]]]]])))


(defn message-view [message categories]

  (let [{:keys [entry_id
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
        message]

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


(defn subscription-row [subscription]

  (clojure.pprint/pprint subscription)

  (let [{:keys [id
                opt_title
                rss_title
                unread_count]}
        subscription

        title
        (or opt_title rss_title)]

    [:div
     {:id (format "uuid-%s-sidebar-item" id)
      :class "feed-item"
      :hx-swap-oob "true"
      :hx-post (html/api-url :viewSubscription
                             {:subscription-id id})
      :hx-trigger "click"
      :hx-target "#content-inner"
      :hx-swap "innerHTML"}
     [:div.feed-title title]
     [:div.feed-unread unread_count]]))
