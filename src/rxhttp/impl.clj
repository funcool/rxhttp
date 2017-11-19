;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2017 Andrey Antukh <niwi@niwi.nz>

(ns rxhttp.impl
  "A streams based http client for clojurescript (browser and node)."
  (:require [clj-http.client :as c]
            [beicon.core :as rx]))

(defn send!
  [{:keys [method url query-string query-params headers body] :as request}
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
         (constantly nil))))))
