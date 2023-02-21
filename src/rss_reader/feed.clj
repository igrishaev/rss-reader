(ns rss-reader.feed
  "
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
  "
  (:import
   java.util.UUID)
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [org.httpkit.client :as client]
   [rss-reader.db :as db]
   [rss-reader.model :as model]
   [rss-reader.rome :as rome]))


(defn get-charset
  ^String [^String content-type]
  (some-> #"(?i)charset\s*=\s*(.+)"
          (re-find content-type)
          (second)
          (str/trim)
          (not-empty)))


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
                  content-type
                  last-modified]}
          headers

          encoding
          (some-> content-type get-charset)]

      (cond

        ;; TODO: don't throw but log & update with error
        error
        (throw error)

        (= status 200)
        (let [reader
              (rome/make-reader body {:lanient true
                                      :encoding encoding
                                      :content-type content-type})
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
              parsed

              category-names
              (map :name categories)

              feed-fields
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
               :sync_count [:raw "sync_count + 1"]
               :sync_date_next [:raw "now() + (interval '1 second' * sync_interval)"]}]

          (model/update-feed feed-id feed-fields)

          (model/upsert-categories feed-id
                                   "feed"
                                   category-names)

          ;; TODO: bulk insert by chunks
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

                  ;; TODO: or (now)
                  ;; TODO: don't throw an exception
                  guid
                  (or link
                      uri
                      (str published-date)
                      (throw (ex-info "AAA" {})))

                  entry-fields
                  {:link (or link uri)
                   :author author
                   :title title
                   :date_published_at published-date
                   :date_updated_at updated-date
                   :summary value}

                  {entry-id :id}
                  (model/upsert-entry feed-id guid entry-fields)

                  category-names
                  (map :name categories)]

              (model/upsert-categories entry-id
                                       "entry"
                                       category-names))))

        ;; TODO: update sync dates
        (= status 304)
        :not-modified

        ;; TODO: log/???
        :else
        :foo))

    (log/errorf "Feed %s not found" feed-id)))


#_
(comment

  (def -feed-id #uuid "2e86f35b-569a-4220-a25b-a646233b1508")

  (update-feed -feed-id)


  )
