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
          @(client/get url_source options)

          {:keys [etag
                  last-modified]}
          headers]

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
                      uri
                      entries]}
              parsed]

          (model/update-feed feed-id
                             {:rss_title title
                              :rss_language language
                              :rss_author author
                              :rss_editor editor
                              :rss_published_at published-date
                              :rss_description description
                              :rss_encoding encoding
                              :rss_feed_type feed-type
                              :url_icon (:url icon)
                              :url_image (:url image)
                              :url_website link
                              :http_status status
                              :http_last_modified last-modified
                              :http_etag etag
                              :sync_count [:raw "sync_count + 1"]})

          (doseq [entry entries]
            (let [{:keys [uri
                          updated-date
                          published-date
                          title
                          author
                          categories
                          link
                          image
                          enclosures
                          description]}
                  entry

                  {:keys [type
                          value]}
                  description

                  guid
                  (or link
                      uri
                      (str published-date)
                      (throw (ex-info "AAA" {})))]

              (model/upsert-entry feed-id guid
                                  {:link (or link uri)
                                   :author author
                                   :date_published_at published-date
                                   :date_updated_at updated-date
                                   :summary value}))))

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


#_
(comment

  (def -feed-id #uuid "2fc19251-d095-454e-86a0-c93ed67d4636")

  (update-feed -feed-id)


  )
