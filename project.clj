(defproject spectator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.flatland/ordered "1.5.7"]
                 [http-kit "2.3.0"]
                 [environ "1.1.0"]
                 [metosin/compojure-api "1.1.11"]
                 [ring/ring-mock "0.4.0"]
                 [cheshire/cheshire "5.13.0"]
                 [org.clojure/data.json "2.5.1"]]
  :main spectator.server
  :plugins [[environ/environ.lein "0.3.1"]
            [lein-ancient "0.6.15"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "spectator.jar"
  :resource-paths ["resources"]
  :target-path "target/%s"
  :profiles {:production {:env {:production true}
                          :resource-paths ["resources"]}})
