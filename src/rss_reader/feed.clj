(ns rss-reader.feed
  "
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
  "
  (:import
   java.util.UUID
   java.util.Date)
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [rss-reader.db :as db]
   [rss-reader.http :as http]
   [rss-reader.model :as model]
   [rss-reader.rome :as rome]))


(def ENTRY-BATCH-SIZE
  50)


(defn by-chunks [coll n]
  (partition n n [] coll))


(defn get-charset
  ^String [^String content-type]
  (some-> #"(?i)charset\s*=\s*(.+)"
          (re-find content-type)
          (second)
          (str/trim)
          (not-empty)))


(defn entry->row [entry]

  (let [{:keys [uri
                ^Date updated-date
                ^Date published-date
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
            (some-> published-date .getTime str)
            (format "none/%s" (random-uuid)))]

    {:guid guid
     :link (or link uri)
     :author author
     :updated_at :%now
     :title title
     :date_published_at published-date
     :date_updated_at updated-date
     :summary value}))


(def feed-sync-fields
  {:sync_count [:raw "sync_count + 1"]
   :sync_date_next [:raw "now() + (interval '1 second' * sync_interval)"]})


(defn feed->row
  [feed]

  (let [{:keys [description
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
        feed]

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
     :url_website link}))


(defn handle-feed-ok
  [feed-id response]

  (let [{:keys [status body headers]}
        response

        {:strs [etag
                content-type
                last-modified]}
          headers

          encoding
          (some-> content-type get-charset)

        reader
        (rome/make-reader body {:lanient true
                                :encoding encoding
                                :content-type content-type})
        feed
        (rome/parse-reader reader)

        {:keys [categories
                entries]}
        feed

        category-names
        (map :name categories)

        feed-fields
        (-> feed
            (feed->row)
            (merge feed-sync-fields)
            (assoc :http_status status
                   :http_last_modified last-modified
                   :http_etag etag))]

    (model/update-feed feed-id feed-fields)

    (model/upsert-categories feed-id
                             "feed"
                             category-names)

    (let [entry-rows
          (map entry->row entries)]

      (doseq [chunk (by-chunks entry-rows ENTRY-BATCH-SIZE)]
        (model/upsert-entries feed-id chunk)))))


(defn handle-feed-not-modified
  [feed-id]
  (let [feed-fields
        (-> {:http_status 304}
            (merge feed-sync-fields))]
    (model/update-feed feed-id feed-fields)))


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
           :throw-exceptions false
           :headers headers}

          {:as response :keys [status]}
          (http/get url_source options)]

      (cond

        (= status 200)
        (handle-feed-ok feed-id response)

        (= status 304)
        (handle-feed-not-modified feed-id)

        ;; TODO: log/???
        :else
        :foo))

    (log/errorf "Feed %s not found" feed-id)))


#_
(comment

  (def -feed-id #uuid "2e86f35b-569a-4220-a25b-a646233b1508")

  (update-feed -feed-id)


  )
