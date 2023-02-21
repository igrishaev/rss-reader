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
   [rss-reader.sanitize :as sanitize]
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

        {description-type :type
         description-text :value}
        description

        url-page
        (or link uri)

        guid
        (or uri
            link
            (some-> published-date .getTime str)
            (format "none/%s" (random-uuid)))]

    {:guid guid
     :link (or link uri)
     :author author
     :updated_at :%now
     :date_published_at published-date
     :date_updated_at updated-date
     :title (some-> title
                    sanitize/sanitize-none)
     :summary (some-> description-text
                      (sanitize/sanitize-html url-page))}))


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

    {:rss_title
     (some-> title
             sanitize/sanitize-none)
     :rss_language language
     :rss_author author
     :rss_editor editor
     :rss_published_at published-date
     :rss_description
     (some-> description
             sanitize/sanitize-none)
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

    (log/infof "Got %s(s) items for feed %s" (count entries) feed-id)

    (model/update-feed feed-id feed-fields)

    (model/upsert-categories feed-id
                             "feed"
                             category-names)

    ;; TODO: save categories
    (let [entry-rows
          (map entry->row entries)]

      (doseq [chunk (by-chunks entry-rows ENTRY-BATCH-SIZE)]
        (model/upsert-entries feed-id chunk)))))


(defn handle-feed-negative
  [feed-id]
  ;; TODO save http status
  ;; TODO log
  )


(defn handle-feed-not-modified
  [feed-id]
  (log/infof "Feed %s has not been modified")
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

        :else
        (handle-feed-negative feed-id)))

    (log/errorf "Feed %s not found" feed-id)))


;; TODO udpate-feed-safe



#_
(comment

  (def -feed-id #uuid "2e86f35b-569a-4220-a25b-a646233b1508")

  (update-feed #uuid "6607fd58-5ea3-47ff-8188-e997d6a8430b")

  (update-feed #uuid "3ea6c6c0-074a-4714-9085-d2d38c1862e9")


  )
