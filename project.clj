(defproject rss-reader "0.1.0-SNAPSHOT"

  :description
  "FIXME: write description"

  :url
  "http://example.com/FIXME"

  :plugins
  [[migratus-lein "0.7.3"]]

  :dependencies
  [[org.clojure/clojure "1.11.1"]

   ;; db
   [com.github.seancorfield/next.jdbc "1.3.847"]
   [org.postgresql/postgresql "42.5.3"]
   [com.github.seancorfield/honeysql "2.4.980"]
   [hikari-cp "3.0.1"]
   [migratus "1.4.9"]

   ;; config
   [aero "1.1.6"]

   ;; state
   [mount "0.1.17"]

   ;; logging
   [org.clojure/tools.logging "1.2.4"]
   [ch.qos.logback/logback-classic "1.4.5"]
   [io.sentry/sentry-logback "6.14.0"]

   ;; http
   [http-kit "2.7.0-alpha1"]
   [ring/ring-core "1.9.6"]
   [compojure "1.7.0"]
   [ring/ring-json "0.5.1"]
   [clj-http "3.12.3"]

   ;; rss
   [com.rometools/rome "1.19.0"]

   ;; json
   [cheshire "5.11.0"]

   ;; sanitize
   [org.jsoup/jsoup "1.15.3"]

   ;; html
   [selmer "1.12.55"]

   ;; signals
   [spootnik/signal "0.2.4"]

   ;; url validator
   [commons-validator/commons-validator "1.7"]]

  :target-path
  "target/%s"

  :profiles
  {:dev
   {:main dev
    :source-paths ["env/dev/src"]
    :resource-paths ["env/dev/resources"]}

   :uberjar
   {:aot :all
    :main rss-reader.main
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
