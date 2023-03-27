(ns rss-reader.htmx.auth
  (:require
   [rss-reader.spec :as spec]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]
   [rss-reader.config :as config]
   [rss-reader.model :as model]
   [rss-reader.db :as db]
   [rss-reader.views :as views]
   [rss-reader.email :as email]
   [rss-reader.html :as html]))


(defn handler [params]

  (let [params-ok
        (s/conform ::spec/api-form-auth params)]

    (if (s/invalid? params-ok)

      (html/response
       (views/form-auth
        {:email (:email params)
         :email-error "Wrong email passed"}))

      (let [{:keys [email]}
            params-ok

            to
            email

            subject
            "Hello"

            {:keys [id]}
            (db/add-auth-code {:email email})

            base-url
            (:base-url config/config)

            url-auth
            (format "%s/auth?id=%s" base-url id)

            body
            (with-out-str
              (println "Hello!")
              (println)
              (println "Please authorize by following the link below. It will expire in a few minutes.")
              (println)
              (println url-auth)
              (println)
              (println)
              (println "Cheers,")
              (println "RSSS.dev"))]

        (try
          (email/send to subject body)
          (html/response
           (views/form-auth-sent))

          (catch Throwable e
            (log/errorf e "Couldn't send email, to: %s" to)
            (html/response
             (views/form-auth
              {:email (:email params)
               :email-error "We couldn't send an email to this address."}))))))))
