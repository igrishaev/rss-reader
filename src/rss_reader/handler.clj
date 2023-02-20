(ns rss-reader.handler
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [compojure.route :as route]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.json :refer
    [wrap-json-body
     wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]))


(defn index [request]
  {:status 200
   :body {:aaa 42
          :ccc [1 2 3 4]}})


(defroutes routes
  (GET "/" request (index request))
  (route/not-found "Not found"))


(def app
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-session)
      (wrap-cookies)))
