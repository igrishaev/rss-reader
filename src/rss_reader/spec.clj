(ns rss-reader.spec
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]))


(s/def ::->uuid
  (s/and string?
         (s/conformer
          (fn [string]
            (or (parse-uuid string)
                ::s/invalid)))))

(s/def ::id ::->uuid)

(s/def ::ne-string
  (s/and string? (complement str/blank?)))


(s/def ::user-id ::->uuid)
(s/def ::subscription-id ::->uuid)
(s/def ::message-id ::->uuid)

(s/def ::email
  (s/and string?
         (s/conformer str/trim)
         (complement str/blank?)
         (fn [string]
           (str/includes? string "@"))
         (s/conformer str/lower-case)))


(s/def ::api-view-subscription
  (s/keys :req-un [::subscription-id]))


(s/def ::api-view-message
  (s/keys :req-un [::message-id]))


(s/def ::api-delete-subscription
  (s/keys :req-un [::subscription-id]))


(s/def ::api-form-auth
  (s/keys :req-un [::email]))


(s/def ::api-auth-code
  (s/keys :req-un [::id]))
