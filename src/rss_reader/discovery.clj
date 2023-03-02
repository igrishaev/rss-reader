(ns rss-reader.discovery
  (:import
   java.io.InputStream
   org.jsoup.Jsoup
   org.jsoup.nodes.Document
   org.jsoup.nodes.Element)
  (:require
   [rss-reader.http :as http]
   [rss-reader.util :as util]))


(def feed-selectors
  ["link[rel='alternate'][type='application/rss+xml']"
   "link[rel='alternate'][type='application/atom+xml']"])


(defn get-rss-links-unsafe [^String url]

  (let [response
        (http/get url {:as :stream
                       :throw-exceptions false})

        {:keys [status body headers]}
        response

        {:strs [content-type]}
        headers

        charset
        (some-> content-type util/get-charset)]

    (when (= status 200)

      (let [doc
            (Jsoup/parse ^InputStream body charset url)

            select
            (fn [^String selector]
              (.select doc selector))

            elements
            (mapcat select feed-selectors)

            hrefs
            (for [^Element el elements]
              (.absUrl el "href"))]

        (-> hrefs set not-empty)))))


(defn get-rss-links [^String url]
  (try
    (get-rss-links-unsafe url)
    (catch Throwable _)))
