;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2017 Andrey Antukh <niwi@niwi.nz>

(ns rxhttp.node
  "A streams based http client for clojurescript (browser and node)."
  (:require [cljs.nodejs :as node]
            [beicon.core :as rx]
            [rxhttp.browser :refer [translate-method normalize-headers]]))

(def ^:private node-http (node/require "http"))
(def ^:private node-https (node/require "https"))
(def ^:private node-url (node/require "url"))
(def ^:private node-qs (node/require "querystring"))
;; Try import form-data for file uploads support in nodejs
#_(def ^:private node-formdata (node/require "form-data"))

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

(defn success?
  "Check if the provided response has a SUCCESS status code."
  [{:keys [status]}]
  (<= 200 status 299))

(defn server-error?
  "Check if the provided response has a SERVER ERROR status code."
  [{:keys [status]}]
  (<= 500 status 599))

(defn client-error?
  "Check if the provided response has a CLIENT ERROR status code."
  [{:keys [status]}]
  (<= 400 status 499))

(defn send!
  "Send a http request and return a `Observable` that will emit the
  response when it is returned by the server. This is a lazy
  operation, this means that until you will not subscribe to the
  resulting observable no request will be fired.

  This is an example of a complete request hash-map:

    {:method :put                    ;; (can be :get :post :put :delete :head :options)
     :headers {}                     ;; (a map of key values that will be sent as headers)
     :url \"http://httpbin.org/get\" ;; (a destination url)
     :query-params {:q \"foo\"}      ;; (a hash-map with query params)
     :query-string \"q=bar\"         ;; (a string with query params if you want raw access to it)
     :body \"foobar\"}               ;; (a body if proceed, can be anything that the underlying
                                         platform can accept: FormData, string, Buffer, blob, ...)

  This method accept and additional optional parameter for provide
  some additional options:

    - `:timeout`:       a number of milliseconds after which the client will
                        close the observable with timeout error.
    - `:credentials?`:  specify if allow send cookies when making a request to
                        a different domain (browser only).
    - `:response-type`  specify the type of the body in the response, can be
                        `:text` and `:blob` (in nodejs `:blob` means that the body
                        will be a instance of `Buffer).

  Here an example of using this method:

    (-> (http/send! {:method :get :url \"https://httpbin.org/get\"})
        (rx/subscribe (fn [response]
                        (println \"Response:\" response))))
  "
  ([request] (send! request {}))
  ([{:keys [method url query-string query-params headers body]
     :or {headers {} method :get}
     :as request}
    {:keys [timeout response-type]
     :or {timeout 0  response-type :text}}]
   (let [headers (merge headers
                        (when (= method :post) {:content-length (count body)}))
         urlopts (merge (build-uri-opts url query-string query-params)
                        {:headers (clj->js headers)
                         :method (translate-method method)})
         nodemod (if (= "https:" (:protocol urlopts)) node-https node-http)]
     (rx/create
      (fn [sink]
        (letfn [(listen [target event callback]
                  (.on target event callback)
                  target)
                (build-response [rsp body]
                  (let [headers (apply js-obj (.-rawHeaders rsp))]
                    {:status (.-statusCode rsp)
                     :body body
                     :headers (normalize-headers headers)}))
                (on-response [rsp]
                  (let [chunks (array)]
                    (listen rsp "readable" #(some->> (.read rsp) (.push chunks)))
                    (listen rsp "end" (fn []
                                        (let [body (cond-> (.concat js/Buffer chunks)
                                                     (= response-type :text) (.toString "utf8"))]
                                          (sink (rx/end (build-response rsp body))))))))
                (on-timeout []
                  (sink (ex-info "Request timeout" {:type type})))
                (on-error [err]
                  (sink err))]
          (let [req (.request nodemod (clj->js urlopts))]
            (listen req "response" on-response)
            (listen req "timeout" on-timeout)
            (listen req "clientError" on-error)
            (listen req "error" on-error)
            (.setTimeout req timeout)
            (when body
              (.write req body))
            (.end req)
            #(.abort req))))))))
