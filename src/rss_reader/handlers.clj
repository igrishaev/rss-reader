(ns rss-reader.handlers
  (:require
   [rss-reader.html :as html]
   [rss-reader.views :as views]))


(defn index [request]
  (html/response
   (views/index nil)))


(defn auth [{:keys [session]}]

  (let [user
        {:id (random-uuid)
         :foo 123
         :bar [1 2 3]}]

    {:status 307
     :headers {"Location" "/"}
     :session (assoc session :user user)}))
