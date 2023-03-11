(ns rss-reader.app
  (:require
   [rss-reader.config :refer [config]]
   [rss-reader.handlers :as handlers]
   [rss-reader.htmx :as htmx]
   [ring.middleware.session.cookie :as cookie]
   [ring.middleware.resource :refer
    [wrap-resource]]
   [ring.middleware.json :refer
    [wrap-json-body
     wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]))


(defn routes
  [{:as request
    :keys [uri
           session
           request-method]}]

  (case [request-method uri]

    [:get "/"]
    (handlers/index request)

    [:post "/htmx"]
    (htmx/handler request)

    [:get "/auth"]
    (handlers/auth request)

    ;; else
    {:status 404
     :headers {"content-type" "text/html"}
     :body "Not found"}))


(defn app []

  (let [session-key
        (.getBytes ^String (:session-key config))

        session-store
        (cookie/cookie-store {:key session-key})]

    (-> routes
        (wrap-keyword-params)
        (wrap-params)
        (wrap-resource "static")
        (wrap-json-body {:keywords? true})
        (wrap-json-response)
        (wrap-session {:store session-store
                       :cookie-name "ring-session"
                       :cookie-attrs {:http-only true}}))))
