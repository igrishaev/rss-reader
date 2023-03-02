(ns rss-reader.google
  "
  https://developers.google.com/custom-search/v1/reference/rest/v1/cse/list
  https://developers.google.com/custom-search/docs/xml_results_appendices#countryCodes
  "
  (:require
   [rss-reader.http :as http]
   [rss-reader.config :as config]))


(defn search

  ([term]
   (search term nil))

  ([term {:keys [country limit]}]

   (let [{:keys [google]}
         config/config

         {:keys [cx key]}
         google

         query-params
         (cond-> {:cx cx
                  :key key
                  :q term}

           country
           (assoc :gl country)

           limit
           (assoc :num limit))

         url
         "https://www.googleapis.com/customsearch/v1"

         params
         {:query-params query-params
          :throw-exceptions false
          :as :json
          :coerce :always}

         {:keys [status body]}
         (http/get url params)]

     (if (= status 200)
       (->> body :items (map :link))
       (throw (ex-info "Google Search error"
                       {:term term
                        :status status
                        :body (when (map? body)
                                body)}))))))
