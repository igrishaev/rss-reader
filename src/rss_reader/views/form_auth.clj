(ns rss-reader.views.form_auth
  (:require
   [rss-reader.html :as html]))


(defn view []
  [:div
   [:h1 "Sign in by Email"]
   [:p "No password is required. We'll send you a magic link to authorize."]
   [:form
    [:input.form-input
     {:type "email"
      :name "email"
      :placeholder "user@domain.com"}]
    [:div.dialog-buttons
     [:div.dialog-button-normal
      "Send the link"]]]])
