(ns rss-reader.feed
  (:require
   [rss-reader.db :as db]
   [clojure.tools.logging :as log]
   )

  )


(defn update-feed [feed-id]

  (if-let [feed
           (db/get-feed-by-id feed-id)]

    (let [{:keys [url_source
                  http_etag
                  http_last_modified]}
          feed

          headers
          (cond-> {:user-agent "foobar"}

            etag
            (assoc :etag http_etag)

            http_last_modified
            (assoc :modified http_last_modified))

          options
          {:headers headers}

          response
          @(http/get url_source
                     {:headers options})]

      )



    (log/errorf "Feed %s not found" feed-id))

  ;; get feed
  ;; http (etag, last-modified)
  ;; 200
  ;; get encoding
  ;; xml?
  ;; - rome-parse
  ;; - upsert entries
  ;; - update feed
  ;; json?
  ;; - remap

  )
