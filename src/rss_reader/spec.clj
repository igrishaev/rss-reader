(ns rss-reader.spec
  (:require
   [clojure.spec.alpha :as s]))


(s/def ::uuid
  (s/conformer parse-uuid))


(s/def ::user-id ::uuid)
(s/def ::subscription-id ::uuid)
(s/def ::message-id ::uuid)

(s/def ::email string?)


(s/def ::api-view-subscription
  (s/keys :req-un [::subscription-id]))


(s/def ::api-view-message
  (s/keys :req-un [::message-id]))


(s/def ::api-delete-subscription
  (s/keys :req-un [::subscription-id]))


(s/def ::api-form-auth
  (s/keys :req-un [::email]))
