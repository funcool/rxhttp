;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2017 Andrey Antukh <niwi@niwi.nz>

(ns rxhttp.core
  "A streams based http client for clojurescript (browser and node)."
  (:require [rxhttp.impl :as impl]
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
  ([request]
   (send! request nil))
  ([request options]
   (impl/send! request options)))
