(ns rss-reader.handlers
  (:require
   [rss-reader.html :as html]
   [rss-reader.views :as views]))


(defn index [request]
  (html/response
   (views/index nil)))


(defn auth [{:keys [cookies]}]

  (let [user
        {:foo 123
         :bar [1 2 3]}]

    {:status 307
     :headers {"Location" "/"}
     :cookies (assoc cookies
                     :user
                     {:value user
                      ;; :secure true
                      ;; :http-only true

                      })}))
