(ns rss-reader.url
  (:import
   java.net.URL
   org.apache.commons.validator.UrlValidator
   org.apache.commons.validator.routines.DomainValidator))


(def ^UrlValidator validator
  (new UrlValidator
       ^"[Ljava.lang.String;"
       (into-array String  ["http" "https"])))


(defn url? [^String url]
  (.isValid validator url))


(defn domain? [^String string]
  (let [validator (DomainValidator/getInstance false)]
    (.isValid validator string)))
