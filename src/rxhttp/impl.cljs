;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2017 Andrey Antukh <niwi@niwi.nz>

(ns rxhttp.impl
  "A streams based http client for clojurescript (browser and node)."
  (:require [clojure.string :as str]
            [beicon.core :as rx]
            [goog.events :as events])
  (:import [goog.net ErrorCode EventType]
           [goog.net.XhrIo ResponseType]
           [goog.net XhrIo]
           [goog.Uri QueryData]
           [goog Uri]))

(def ^:dynamic *xhr-impl* XhrIo)

;; Import all node stuff if the build is for nodejs
(when (= *target* "nodejs")
  (def ^:private node-http (node/require "http"))
  (def ^:private node-https (node/require "https"))
  (def ^:private node-url (node/require "url"))
  (def ^:private node-qs (node/require "querystring"))
  ;; Try import form-data for file uploads support in nodejs
  #_(def ^:private node-formdata (node/require "form-data")))

(defn- translate-method
  "Translates the keyword method name to internal http method naming."
  [method]
  (case method
    :head    "HEAD"
    :options "OPTIONS"
    :get     "GET"
    :post    "POST"
    :put     "PUT"
    :patch   "PATCH"
    :delete  "DELETE"
    :trace   "TRACE"))

(defn- normalize-headers
  "Normalize all headers into a lowe-case keys map."
  [headers]
  (reduce-kv (fn [acc k v]
               (assoc acc (str/lower-case k) v))
             {} (js->clj headers)))

(defn- translate-error-code
  [code]
  (condp = code
    ErrorCode.TIMEOUT    :timeout
    ErrorCode.EXCEPTION  :exception
    ErrorCode.HTTP_ERROR :http
    ErrorCode.ABORT      :abort))

(defn- translate-response-type
  [type]
  (case type
    :text ResponseType.TEXT
    :blob ResponseType.BLOB
    ResponseType.DEFAULT))

(defn- build-uri
  "Build a XhrIo compatible uri string."
  [url qs qp]
  (let [uri (Uri. url)]
    (when qs (.setQuery uri qs))
    (when qp
      (let [dt (.createFromMap QueryData (clj->js  qp))]
        (.setQueryData uri dt)))
    (.toString uri)))

(defn- build-uri-opts
  "Build a nodejs http compatible options map."
  [url qs qp]
  (let [parsed (.parse node-url url)
        path (cond
               (string? qs) (str (.-pathname parsed) "?" qs)
               (map? qp) (str (.-pathname parsed) "?" (.stringify node-qs (clj->js qp)))
               :else (.-pathname parsed))]
    {:protocol (.-protocol parsed)
     :host (.-hostname parsed)
     :port (.-port parsed)
     :path path}))

(defn- fetch-browser
  [{:keys [method url query-string query-params headers body] :as request}
   {:keys [timeout credentials? response-type]
    :or {timeout 0 credentials? false response-type :text}}]
  (let [uri (build-uri url query-string query-params)
        headers (if headers (clj->js headers) #js {})
        method (translate-method method)
        impl *xhr-impl*]
    (rx/create
     (fn [sink]
       (let [xhr (.send impl uri nil method body headers timeout credentials?)]
         (.setResponseType xhr (translate-response-type response-type))
         (events/listen xhr EventType.COMPLETE
                        (fn []
                          (if (or (= (.getLastErrorCode xhr) ErrorCode.HTTP_ERROR)
                                  (.isSuccess xhr))
                            (let [rsp {:status (.getStatus xhr)
                                       :body (.getResponse xhr)
                                       :headers (normalize-headers
                                                 (.getResponseHeaders xhr))}]
                              (sink (rx/end rsp)))
                            (let [type (-> (.getLastErrorCode xhr)
                                           (translate-error-code))
                                  message (.getLastError xhr)]
                              (sink (ex-info message {:type type}))))))
         #(.abort xhr))))))

;; (defn fetch-node
;;   [{:keys [method url query-string query-params headers body]
;;     :or {headers {} method :get}
;;     :as request}
;;    {:keys [timeout response-type]
;;     :or {timeout 0  response-type :text}}]
;;   (let [headers (merge headers
;;                        (when (= method :post) {:content-length (count body)}))
;;         urlopts (merge (build-uri-opts url query-string query-params)
;;                        {:headers (clj->js headers)
;;                         :method (translate-method method)})
;;         nodemod (if (= "https:" (:protocol urlopts)) node-https node-http)]
;;     (rx/create
;;      (fn [sink]
;;        (letfn [(listen [target event callback]
;;                  (.on target event callback)
;;                  target)
;;                (build-response [rsp body]
;;                  (let [headers (apply js-obj (.-rawHeaders rsp))]
;;                    {:status (.-statusCode rsp)
;;                     :body body
;;                     :headers (normalize-headers headers)}))
;;                (on-response [rsp]
;;                  (let [chunks (array)]
;;                    (listen rsp "readable" #(some->> (.read rsp) (.push chunks)))
;;                    (listen rsp "end" (fn []
;;                                        (let [body (cond-> (.concat js/Buffer chunks)
;;                                                     (= response-type :text) (.toString "utf8"))]
;;                                          (sink (rx/end (build-response rsp body))))))))
;;                (on-timeout []
;;                  (sink (ex-info "Request timeout" {:type type})))
;;                (on-error [err]
;;                  (sink err))]
;;          (let [req (.request nodemod (clj->js urlopts))]
;;            (listen req "response" on-response)
;;            (listen req "timeout" on-timeout)
;;            (listen req "clientError" on-error)
;;            (listen req "error" on-error)
;;            (.setTimeout req timeout)
;;            (when body
;;              (.write req body))
;;            (.end req)
;;            #(.abort req)))))))

(defn send!
  [request options]
  (fetch-browser request options))
