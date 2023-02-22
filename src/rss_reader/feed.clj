(ns rss-reader.feed
  "
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
  "
  (:import
   java.util.Date
   java.util.UUID)
  (:require
   [clojure.tools.logging :as log]
   [rss-reader.const :as c]
   [rss-reader.db :as db]
   [rss-reader.http :as http]
   [rss-reader.model :as model]
   [rss-reader.rome :as rome]
   [rss-reader.sanitize :as sanitize]
   [rss-reader.util :as util]))


(def feed-sync-fields
  {:sync_count [:raw "sync_count + 1"]
   :sync_date_next [:raw "now() + (interval '1 second' * sync_interval)"]})


(def feed-ok-fields
  (merge feed-sync-fields
         {:err_attempts 0
          :err_class nil
          :err_message nil}))


(defn feed-err-fields [e]
  (merge feed-sync-fields
         {:err_attempts [:raw "err_attempts + 1"]
          :err_class (-> e class .getName)
          :err_message (-> e ex-message)}))


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


(defn categories->names
  [categories]
  (set (map :name categories)))


(defn handle-feed-ok
  [feed-id response]

  (let [{:keys [status body headers]}
        response

        {:strs [etag
                content-type
                last-modified]}
        headers

        encoding
        (some-> content-type util/get-charset)

        reader
        (rome/make-reader body {:lanient      true
                                :encoding     encoding
                                :content-type content-type})
        feed
        (rome/parse-reader reader)

        {:keys [categories
                entries]}
        feed

        category-names
        (categories->names categories)

        feed-fields
        (-> feed
            (feed->row)
            (merge feed-ok-fields)
            (assoc :http_status status
                   :http_last_modified last-modified
                   :http_etag etag))]

    (log/infof "Got %s(s) items for feed %s" (count entries) feed-id)

    (model/update-feed feed-id feed-fields)

    (model/upsert-categories feed-id
                             "feed"
                             category-names)

    (doseq [entries (util/by-chunks entries
                                    c/entry-batch-size)]

      (loop [[e & entries]    entries
             rows             []
             guid->categories {}]

        (let [{:keys [categories]}
              e

              row
              (entry->row e)

              {:keys [guid]}
              row]

          (if entries
            (recur entries
                   (conj rows row)
                   (assoc guid->categories
                          guid
                          (categories->names categories)))

            (let [result
                  (model/upsert-entries feed-id rows)

                  rows-categories
                  (for [{:keys [id guid]}  result
                        [guid' categories] guid->categories
                        category           categories
                        :when (= guid guid')]
                    {:parent-id id
                     :parent-type "entry"
                     :category category})]

              (model/upsert-categories-bulk rows-categories))))))))


(defn handle-feed-not-modified
  [feed-id]
  (log/infof "Feed %s has not been modified")
  (let [feed-fields
        (merge feed-ok-fields
               {:http_status 304})]
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
           :throw-exceptions true
           :headers headers}

          {:as response :keys [status]}
          (http/get url_source options)]

      (cond

        (= status 200)
        (handle-feed-ok feed-id response)

        (= status 304)
        (handle-feed-not-modified feed-id)))

    (log/errorf "Feed %s not found" feed-id)))


(defn update-feed-safe [feed-id]
  (try
    (update-feed feed-id)
    (log/infof "Feed %s has been updated" feed-id)
    (catch Throwable e
      (log/errorf e "Feed %s failed to update" feed-id)
      (model/update-feed feed-id (feed-err-fields e)))))



#_
(comment

  (model/upsert-feed "http://oglaf.com/feeds/rss/")
  (model/upsert-feed "https://habr.com/ru/rss/all/all/?fl=ru")

  (update-feed #uuid "6607fd58-5ea3-47ff-8188-e997d6a8430b")

  (update-feed #uuid "3ea6c6c0-074a-4714-9085-d2d38c1862e9")


  )
