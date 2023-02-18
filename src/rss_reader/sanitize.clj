(ns rss-reader.sanitize
  (:import
   org.jsoup.Jsoup
   org.jsoup.nodes.Document
   [org.jsoup.safety Safelist Cleaner]
   org.jsoup.nodes.Entities$EscapeMode))

(def tags-allowed
  [
   "a"
   "b"
   "blockquote"
   "br"
   "h1"
   "h2"
   "h3"
   "h4"
   "h5"
   "h6"
   "i"
   "iframe"
   "img"
   "li"
   "p"
   "pre"
   "small"
   "span"
   "strike"
   "strong"
   "sub"
   "sup"
   "table"
   "tbody"
   "td"
   "tfoot"
   "th"
   "thead"
   "tr"
   "u"
   "ul"
   ])


(def attrs-allowed
  {"img"
   ["src"]

   "iframe"
   ["src" "allowfullscreen"]

   "a"
   ["href"]})


(def protocols-allowed
  {"a" {"href" ["ftp" "http" "https" "mailto"]}
   "img" {"src" ["http" "https"]}
   "iframe" {"src" ["http" "https"]}})


(defn ->array
  [vals]
  (into-array String vals))


(def whitelist-html
  (let [sl (new Safelist)]

    (.addTags sl (->array tags-allowed))

    (doseq [[tag attrs] attrs-allowed]
      (.addAttributes sl tag (->array attrs)))

    (doseq [[tag attr->protocols] protocols-allowed
            [attr protocols] attr->protocols]
      (.addProtocols sl tag attr (->array protocols)))

    sl))


(def XHTML Entities$EscapeMode/xhtml)


(def ^Cleaner cleaner-html
  (new Cleaner whitelist-html))


(def whitelist-none
  (Safelist/none))


(defn sanitize-html
  [^String html ^String url]
  (let [^Document doc (Jsoup/parse html (or url ""))]
    #_(process-iframes src)
    (let [out (.clean cleaner-html doc)]
      (.. out outputSettings (escapeMode XHTML))
      (.. out body html))))


(defn sanitize-none
  [^String html]
  (Jsoup/clean html whitelist-none))
