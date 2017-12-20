;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2017 Andrey Antukh <niwi@niwi.nz>

(ns rxhttp.jvm
  "A streams based http client for clojurescript (browser and node)."
  (:require [clj-http.client :as c]
            [beicon.core :as rx]))

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
  ([{:keys [method url query-string query-params headers body] :as request}
    {:keys [timeout] :or {timeout 60000}}]
   (let [baseopts {:method method
                   :url url
                   :headers headers
                   :body body
                   :decode-cookies false
                   :async? true
                   :socket-timeout timeout
                   :conn-timeout timeout}
         options (merge baseopts
                        (when query-params {:query-params query-params})
                        (when query-string {:query-string query-string}))]
     (rx/create
      (fn [sink]
        (letfn [(on-response [rsp]
                  (let [rsp (select-keys rsp [:body :headers :status])]
                    (sink (rx/end rsp))))
                (on-exception [err]
                  (if (instance? clojure.lang.ExceptionInfo err)
                    (on-response (ex-data err))
                    (sink (rx/end err))))]
          (c/request options on-response on-exception)
          (constantly nil)))))))

(defn send!!
  "A synchronous version of `send!` function."
  [& args]
  (let [result (volatile! nil)
        latch (java.util.concurrent.CountDownLatch. 1)]
    (-> (apply send! args)
        (rx/subscribe #(vreset! result %)
                      #(do (vreset! result %)
                           (.countDown latch))
                      #(.countDown latch)))
    (.await latch)
    (let [result (deref result)]
      (if (instance? Throwable result)
        (throw result)
        result))))
