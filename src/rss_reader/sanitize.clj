(ns rss-reader.sanitize
  (:import
   org.jsoup.Jsoup
   org.jsoup.nodes.Document
   org.jsoup.nodes.Element
   org.jsoup.nodes.Entities$EscapeMode
   org.jsoup.safety.Cleaner
   org.jsoup.safety.Safelist))


(def tags-allowed
  [
   "a"
   "article"
   "b"
   "blockquote"
   "br"
   "figcaption"
   "figure"
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
   "main"
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


(def re-youtube
  #"(?i)youtube.com/embed")

(def re-vk
  #"(?i)vk.com/video_ext.php")

(def re-coube
  #"(?i)coub.com/embed")

(def re-soundcloud
  #"(?i)soundcloud.com/player")

(def re-vimeo
  #"(?i)player.vimeo.com/video")


(defn media-src?
  [^String src]
  (or (re-find re-youtube src)
      (re-find re-vk src)
      (re-find re-coube src)
      (re-find re-soundcloud src)
      (re-find re-vimeo src)))


(defn process-iframes
  [^Document doc]
  (doseq [^Element el (.select doc "iframe")]
    (let [src (.absUrl el "src")]
      (when-not (media-src? src)
        (.remove el)))))


(defn sanitize-html
  [^String html ^String url]
  (let [^Document doc (Jsoup/parse html (or url ""))]
    (process-iframes doc)
    (let [out (.clean cleaner-html doc)]
      (.. out outputSettings (escapeMode XHTML))
      (.. out body html))))


(defn sanitize-none
  [^String html]
  (Jsoup/clean html whitelist-none))
