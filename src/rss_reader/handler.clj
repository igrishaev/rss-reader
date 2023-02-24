(ns rss-reader.handler
  (:require
   [rss-reader.index :as index]
   [rss-reader.htmx :as htmx]
   [ring.middleware.cookies :refer [wrap-cookies]]
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
    :keys [request-method uri]}]
  (case [request-method uri]

    [:get "/"]
    (index/handler request)

    [:post "/htmx"]
    (htmx/handler request)

    ;; else
    {:status 404
     :headers {"content-type" "text/html"}
     :body "Not found"}))


(def handler
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-resource "static")
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-session)
      (wrap-cookies)))
