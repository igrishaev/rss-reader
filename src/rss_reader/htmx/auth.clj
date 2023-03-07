(ns rss-reader.htmx.auth
  (:require
   [rss-reader.views :as views]
   [rss-reader.email :as email]
   [rss-reader.html :as html]

))


(defn handler [{:keys [email]}]

  (let [to
        email

        subject
        "hello"

        body
        (with-out-str
          (println "AAAAAAAA")
          (println "BBBBBBBB")
          (println "CCCCCCCC"))]

    #_
    (email/send to subject body)

    (html/response
     (views/form-auth-ok))))
