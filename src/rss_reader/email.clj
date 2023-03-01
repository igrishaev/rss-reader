(ns rss-reader.email
  (:require
   [postal.core :as postal]
   [rss-reader.config :refer [config]]))


(defn send-mail
  [to subject body]

  (let [{:keys [smtp]}
        config

        {:keys [from
                host
                user
                port
                pass]}
        smtp

        response
        (postal/send-message
         {:host host
          :user user
          :pass pass
          :port port
          :tls true}
         {:from from
          :to to
          :subject subject
          :body body})

        {:keys [code
                error
                message]}
        response]

    (when-not (zero? code)
      (throw (ex-info (format "SMTP error, code: %s, error: %s, message: %s"
                              code error message)
                      {:type ::error
                       :code code
                       :error error
                       :message message
                       :to to
                       :subject subject})))))
