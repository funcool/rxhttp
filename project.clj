(defproject funcool/rxhttp "1.0.0-SNAPSHOT"
  :description "A stream based http client for Clojure and ClojureScript"
  :url "http://funcool.github.io/rxhttp"
  :license {:name "MPL 2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [clj-http "3.7.0"]
                 [funcool/beicon "4.1.0"]]
  :profiles
  {:dev
   {:dependencies [[cheshire "5.8.0"]]
    :plugins [[lein-ancient "0.6.10"]]}})
