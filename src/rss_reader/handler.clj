(ns rss-reader.handler
  (:require
   [reitit.core :as r]
   [reitit.ring :as ring]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.json :refer
    [wrap-json-body
     wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.resource :refer
    [wrap-resource]]
   [ring.middleware.session :refer [wrap-session]]
   rss-reader.handlers.index))



(def router
  (ring/router
   [["/"
     {:name ::index
      :get rss-reader.handlers.index/handler}]
    ["/foo/bar"
     {:name ::foobar
      :get rss-reader.handlers.index/dialog}]]))


(def routes
  (ring/ring-handler
   router
   (route/not-found "Not found")))


(defn back-url
  ([route-name]
   (back-url route-name nil))

  ([route-name path-params]
   (-> router
       (r/match-by-name route-name path-params)
       (r/match->path))))


(def handler
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-resource "static")
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-session)
      (wrap-cookies)))
