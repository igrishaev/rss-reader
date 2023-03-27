(ns rss-reader.handlers
  (:require
   [clojure.spec.alpha :as s]
   [rss-reader.spec :as spec]
   [rss-reader.model :as model]
   [rss-reader.db :as db]
   [rss-reader.html :as html]
   [rss-reader.views :as views]))


(defn index [request]
  (html/response
   (views/index nil)))


(defn auth [request]

  (let [{:keys [params
                session]}
        request

        {:keys [user]}
        session

        params-ok
        (s/conform ::spec/api-auth-code params)]

    (if (s/invalid? params-ok)

      (html/response
       (views/index user {:message "The id parameter is malformed or missing."}))

      (let [{:keys [id]}
            params-ok]

        (if-let [{:keys [email]}
                 (db/get-auth-code-by-id {:id id})]

          (let [user
                (db/upsert-user {:email email})

                session-user
                (select-keys user [:id :email])]

            {:status 307
             :headers {"Location" "/"}
             :session (assoc session :user session-user)})

          (html/response
           (views/index user {:message "The authentication link has been expired."})))))))
