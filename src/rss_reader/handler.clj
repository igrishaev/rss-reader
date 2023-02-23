(ns rss-reader.handler
  (:require
   rss-reader.handlers.index
   [compojure.core :refer [defroutes GET POST]]
   [compojure.route :as route]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.resource :refer
    [wrap-resource]]
   [ring.middleware.json :refer
    [wrap-json-body
     wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]))


(defroutes routes
  (GET "/" request (rss-reader.handlers.index/handler request))
  (GET "/foo/bar" request (rss-reader.handlers.index/dialog request))
  #_
  (GET "/htmx/subscription/" request (rss-reader.handlers.index/handler request))

  (route/not-found "Not found"))


(def handler
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-resource "static")
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-session)
      (wrap-cookies)))
