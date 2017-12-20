(ns rxhttp.test-jvm
  (:require [clojure.test :as t]
            [beicon.core :as rx]
            [rxhttp.jvm :as http]
            [cheshire.core :as json])
  (:import java.util.concurrent.CountDownLatch))

;; --- helpers

(defn drain!
  [ob]
  (let [result (volatile! nil)
        latch (CountDownLatch. 1)]
    (rx/subscribe ob
                   #(vreset! result %)
                   #(do
                      (vreset! result %)
                      (.countDown latch))
                   #(.countDown latch))
    (.await latch)
    @result))

;; --- jvm tests

(t/deftest send-plain-get
  (let [url "https://httpbin.org/get?foo=bar"
        req {:method :get
             :query-params {:foo "bar"}
             :url url}
        rsp (drain! (http/send! req))]
    (t/is (string? (:body rsp)))
    (t/is (= (:status rsp) 200))
    (t/is (= (get-in rsp [:headers "content-type"]) "application/json"))))

(t/deftest send-plain-get-with-client-error
  (let [url "https://httpbin.org/status/400"
        req {:method :get :url url}
        rsp (drain! (http/send! req))]
    (t/is (string? (:body rsp)))
    (t/is (= (:status rsp) 400))))

(t/deftest send-plain-get-with-server-error
  (let [url "https://httpbin.org/status/500"
        req {:method :get :url url}
        rsp (drain! (http/send! req))]
    (t/is (string? (:body rsp)))
    (t/is (= (:status rsp) 500))))

(t/deftest send-plain-get-with-timeout
  (let [url "https://httpbin.org/delay/3"
        req {:method :get :url url}
        opts {:timeout 1000}
        rsp (drain! (http/send! req opts))]
    (t/is (instance? java.net.SocketTimeoutException rsp))))

(t/deftest send-plain-post
  (let [url "https://httpbin.org/post"
        req {:method :post
             :headers {:content-type "application/json"}
             :body (json/encode {:foo "bar"})
             :url url}
        rsp (drain! (http/send! req))]
    (t/is (string? (:body rsp)))
    (t/is (= (:status rsp) 200))
    (let [body (json/decode (:body rsp) true)]
      (t/is (= (:body req) (:data body))))))

(t/deftest send-sync-plain
  (let [url "https://httpbin.org/get?foo=bar"
        req {:method :get
             :query-params {:foo "bar"}
             :url url}
        rsp (http/send!! req)]
    (t/is (string? (:body rsp)))
    (t/is (= (:status rsp) 200))
    (t/is (= (get-in rsp [:headers "content-type"]) "application/json"))))
