(ns rxhttp.test-browser
  (:require [cljs.test :as t]
            [beicon.core :as rx]
            [rxhttp.impl :as impl]
            [rxhttp.core :as http])
  (:import goog.testing.net.XhrIo))

;; --- helpers

(defn get-last-raw-request
  []
  (aget (.getSendInstances XhrIo) 0))

(defn get-last-request
  []
  (let [r (get-last-raw-request)]
    {:method  (.getLastMethod r)
     :url     (.toString (.getLastUri r))
     :headers (js->clj (.getLastRequestHeaders r))
     :body    (.getLastContent r)}))

(defn simulate-response!
  ([status]
   (simulate-response! status nil))
  ([status content]
   (simulate-response! status content {}))
  ([status content headers]
   (-> (get-last-raw-request)
       (.simulateResponse status content (clj->js headers)))))

(defn simulate-timeout!
  []
  (-> (get-last-raw-request)
      (.simulateTimeout)))

(defn after-each
  []
  (set! *target* "nodejs")
  (.cleanup goog.testing.net.XhrIo))

(defn before-each
  []
  (set! *target* "default"))

(defn send!
  [& args]
  (binding [impl/*xhr-impl* goog.testing.net.XhrIo]
    (apply http/send! args)))

(defn drain!
  ([ob cb]
   (drain! ob cb (fn [err]
                   (js/console.log err))))
  ([ob cb ecb]
   (let [values (volatile! [])]
     (rx/subscribe ob
                   #(vswap! values conj %)
                   #(ecb %)
                   #(cb (first @values))))))

;; --- browser mocked tests

(t/use-fixtures :each {:after after-each :before before-each})

(t/deftest send-plain-get
  (t/async done
    (let [url "http://localhost/test"
          req {:method :get :url url}
          rsp (send! req)]
      (drain! rsp (fn [rsp]
                    (let [req' (get-last-request)]
                      (t/is (= (:method req') "GET"))
                      (t/is (= (:url req') url))
                      (t/is (= (:body req') nil)))
                    (t/is (= (:status rsp) 201))
                    (t/is (= (:body rsp) "foobar"))
                    (t/is (= (:headers rsp) {}))
                    (done)))
      (simulate-response! 201 "foobar"))))

(t/deftest send-post-with-headers
  (t/async done
    (let [url "http://localhost/test"
          req {:method :post
               :url url
               :headers {:content-type "text/plain"}
               :query-params {:foo "bar"}
               :body "foobar"}
          rsp (send! req)]
      (drain! rsp (fn [rsp]
                    (let [req' (get-last-request)]
                      (t/is (= (:method req') "POST"))
                      (t/is (= (:url req') "http://localhost/test?foo=bar"))
                      (t/is (= (:body req') "foobar"))
                      (t/is (= (:headers req') {"content-type" "text/plain"})))
                    (t/is (= (:status rsp) 400))
                    (t/is (= (:body rsp) ""))
                    (t/is (= (:headers rsp) {"content-type" "text/plain"}))
                    (done)))
      (simulate-response! 400 "" {:content-type "text/plain"}))))

(t/deftest send-get-with-timeout
  (t/async done
    (let [url "http://localhost/test"
          req {:method :get :url url}
          rsp (send! req)]
      (drain! rsp
              (fn [rsp]
                (t/is (= rsp nil)))
              (fn [err]
                (t/is (instance? cljs.core.ExceptionInfo err))
                (t/is (= (ex-data err) {:type :timeout}))
                (done)))
      (simulate-timeout!))))
