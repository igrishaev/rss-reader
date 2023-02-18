(ns rss-reader.feed
  "
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
  "
  (:import
   java.util.UUID)
  (:require
   [rss-reader.db :as db]
   [rss-reader.model :as model]
   [rss-reader.rome :as rome]
   [org.httpkit.client :as client]
   [clojure.tools.logging :as log]))


(defn update-feed [feed-id]

  (if-let [feed
           (model/get-feed-by-id feed-id)]

    (let [{:keys [url_source
                  http_etag
                  http_last_modified]}
          feed

          headers
          (cond-> {"User-Agent" "RSS reader"}

            http_etag
            (assoc "If-None-Match" http_etag)

            http_last_modified
            (assoc "If-Modified-Since" http_last_modified))

          options
          {:as :stream
           :insecure? true
           :headers headers}

          {:keys [status body headers error]}
          @(client/get url_source options)]

      (cond

        error
        (throw error)

        (= status 200)
        (let [reader
              (rome/make-reader body #_{:lanient true
                                        :encoding "aaa"
                                        :content-type "aaa"})
              parsed
              (rome/parse-reader reader)

              {:keys [description
                      encoding
                      feed-type
                      published-date
                      icon
                      title
                      author
                      categories
                      language
                      link
                      editor
                      generator
                      image
                      ]}

              ]

          (spit "feed.edn" (with-out-str
                             (clojure.pprint/pprint
                              parsed)))


          )




        (= status 304)
        :not-modified

        :else
        :foo))

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
