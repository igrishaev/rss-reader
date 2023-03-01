(ns rss-reader.htmx.subscription-row
  (:require
   [rss-reader.html :as html]))


(defn element [subscritpion]

  (let [{:keys [id
                opt_title
                unread_count]}
        subscritpion]

    [:div
     {:id (format "uuid-%s-sidebar-item" id)
      :class "feed-item"
      :hx-swap-oob "true"
      :hx-post (html/api-url :viewSubscription
                             {:subscription-id id})
      :hx-trigger "click"
      :hx-target "#content-inner"
      :hx-swap "innerHTML"}
     [:div.feed-title
      "aaaaaaa" ;; TODO: pass proper params
      #_
      (or opt_title rss_title)]
     [:div.feed-unread
      unread_count]]))
