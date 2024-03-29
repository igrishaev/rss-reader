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
   [rss-reader.url :as url]
   [rss-reader.http :as http]
   [rss-reader.model :as model]
   [rss-reader.rome :as rome]
   [rss-reader.sanitize :as sanitize]
   [rss-reader.util :as util]))


(def feed-sync-fields
  {:sync_count [:raw "sync_count + 1"]
   :sync_date_prev [:raw "now()"]
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


(defn entry->row [feed-id entry]

  (let [{:keys [uri
                ^Date updated-date
                ^Date published-date
                title
                author
                categories
                link
                image
                enclosures ;; TODO: save enclosures
                description]}
        entry

        {description-type :type
         description-text :value}
        description

        url-page
        (or link uri)

        sql-guid
        (or uri
            link
            (some-> published-date .getTime str)
            (format "none/%s" (random-uuid)))

        sql-published-date
        (or published-date
            updated-date)

        sql-link
        (or link uri)]

    {:feed_id feed-id
     :guid sql-guid
     :link sql-link
     :author author
     :date_published_at sql-published-date
     :date_updated_at updated-date
     :title (some-> title sanitize/sanitize-none)
     :summary (some-> description-text (sanitize/sanitize-html url-page))
     :teaser (some-> description-text sanitize/sanitize-none)}))


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

    (let [feed-title
          (or (some-> title sanitize/sanitize-none)
              (some-> link url/get-host))

          feed-description
          (some-> description sanitize/sanitize-none)]

      (cond-> {:rss_language language
               :rss_author author
               :rss_editor editor
               :rss_published_at published-date
               :rss_description feed-description
               :rss_encoding encoding
               :rss_feed_type feed-type
               :url_icon (:url icon)
               :url_image (:url image)
               :url_website link}

        feed-title
        (assoc :rss_title feed-title)))))


(defn categories->names
  [categories]
  (set (map :name categories)))


(def into-set
    (fnil into #{}))


(defn head-feed-from-response [response]
  (let [{:keys [status body headers]}
        response

        {:strs [content-type]}
        headers

        encoding
        (some-> content-type util/get-charset)

        reader-opt
        {:lanient true
         :encoding encoding
         :content-type content-type}

        reader
        (rome/make-reader body reader-opt)]

    (-> reader
        (rome/parse-reader)
        (dissoc :entries))))


(defn head-feed [^String url]
  (let [response
        (http/get url {:as :stream
                       :throw-exceptions true})]
    (head-feed-from-response response)))


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

        reader-opt
        {:lanient true
         :encoding encoding
         :content-type content-type}

        reader
        (rome/make-reader body reader-opt)

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
            (assoc :http_status status
                   :http_last_modified last-modified
                   :http_etag etag))]

    (log/infof "Got %s(s) items for feed %s" (count entries) feed-id)

    (db/sync-feed-ok {:id feed-id
                      :fields feed-fields})

    (model/upsert-categories feed-id
                             "feed"
                             category-names)

    (doseq [entries (util/by-chunks
                     entries
                     c/entry-batch-size)]

      (let [rows
            (for [entry entries]
              (entry->row feed-id entry))

            guid->categories
            (reduce
             (fn [acc [row entry]]
               (let [{:keys [guid]}
                     row
                     {:keys [categories]}
                     entry]
                 (update acc
                         guid
                         into-set
                         (map :name categories))))
             {}
             (map vector rows entries))

            db-result
            (db/upsert-entries {:rows rows})

            id->guid
            (reduce
             (fn [acc {:keys [guid id]}]
               (assoc acc id guid))
             {}
             db-result)

            category-rows
            (for [[id guid1]
                  id->guid

                  [guid2 categories]
                  guid->categories

                  category
                  categories

                  :when (= guid1 guid2)]

              {:parent-id id
               :parent-type "entry"
               :category category})]

        #_
        (model/upsert-categories-bulk category-rows)))))


(defn handle-feed-not-modified
  [feed-id]
  (log/infof "Feed %s has not been modified" feed-id)
  (db/sync-feed-ok {:id feed-id
                    :fields {:http_status 304}}))


(defn update-feed [feed-id]

  (if-let [feed
           (db/get-feed-by-id {:id feed-id})]

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
      (db/sync-feed-err {:id feed-id
                         :err-class (-> e class .getName)
                         :err-message (ex-message e)}))))
