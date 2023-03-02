(ns rss-reader.search
  (:require
   [rss-reader.model :as model]
   [rss-reader.url :as url]
   [rss-reader.http :as http]
   [rss-reader.feed :as feed]
   [rss-reader.google :as google]
   [rss-reader.discovery :as discovery]))


(defn lookup-feeds [term]
  (cond

    (url/url? term)
    (let [response
          (http/get term {:as :stream
                          :throw-exceptions false})]
      (when (http/ok? response)
        (cond
          (http/html? response)
          (discovery/get-rss-links-from-response response)
          (http/feed? response)
          (feed/head-feed-from-response response))))

    (url/domain? term)
    (let [url
          (str "http://" term)
          links
          (discovery/get-rss-links url)]
      (mapv feed/head-feed links))

    :else
    (let [links
          (google/search term {:limit 1})]
      (when-let [link (first links)]
        (let [links
              (discovery/get-rss-links link)]
          (mapv feed/head-feed links))))))


(defn search [term]

  (let [feed-rows
        (model/search-feeds term)

        feed-maps
        (lookup-feeds term)]

    {:rows feed-rows
     :maps feed-maps}))
