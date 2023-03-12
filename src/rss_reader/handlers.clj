(ns rss-reader.handlers
  (:require
   [clojure.spec.alpha :as s]
   [rss-reader.spec :as spec]
   [rss-reader.model :as model]
   [rss-reader.html :as html]
   [rss-reader.views :as views]))


(defn index [request]
  (html/response
   (views/index nil)))


(defn auth [request]

  (let [{:keys [params
                session]}
        request

        params-ok
        (s/conform ::spec/api-auth-code params)]

    (if (s/invalid? params-ok)

      ::wrong-params

      (let [{:keys [id]}
            params-ok]

        (if-let [auth-code
                 (model/get-auth-code-by-id id)]

          (if-let [user
                   (model/get-user-by-id id)]

            {:status 307
             :headers {"Location" "/"}
             :session (assoc session :user user)}

            ::no-user)

          ::no-code)))))
