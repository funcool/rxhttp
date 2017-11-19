(defproject funcool/rxhttp "1.0.0"
  :description "A stream based http client for ClojureScript"
  :url "http://funcool.github.io/rxhttp"
  :license {:name "MPL 2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [funcool/beicon "4.1.0"]]
  :profiles
  {:dev
   {:plugins [[lein-ancient "0.6.10"]]}})
