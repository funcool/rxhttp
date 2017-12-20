(ns rxhttp.test-nodejs
  (:require [cljs.test :as t]
            [beicon.core :as rx]
            [rxhttp.node :as http]))

;; --- helpers

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

;; --- nodejs tests

(t/deftest send-plain-get
  (t/async done
    (let [url "https://httpbin.org/get?foo=bar"
          req {:method :get
               :query-params {:foo "bar"}
               :url url}
          rsp (http/send! req)]
      (drain! rsp (fn [{:keys [status body headers] :as rsp}]
                    (t/is (string? body))
                    (t/is (= status 200))
                    (t/is (= (get headers "content-type") "application/json"))
                    (let [body (js->clj (.parse js/JSON body) :keywordize-keys true)]
                      (t/is (= {:foo "bar"} (:args body))))
                    (done))))))

(t/deftest send-plain-get-and-return-buffer
  (t/async done
    (let [url "https://httpbin.org/get?foo=bar"
          req {:method :get
               :query-params {:foo "bar"}
               :url url}
          rsp (http/send! req {:response-type :blob})]
      (drain! rsp (fn [{:keys [status body headers] :as rsp}]
                    (t/is (instance? js/Buffer body))
                    (t/is (= status 200))
                    (t/is (= (get headers "content-type") "application/json"))
                    (done))))))

(t/deftest send-plain-post
  (t/async done
    (let [url "https://httpbin.org/post"
          req {:method :post
               :headers {:content-type "application/json"}
               :body (.stringify js/JSON (clj->js {:foo "bar"}))
               :url url}
          rsp (http/send! req {:response-type :text})]
      (drain! rsp (fn [{:keys [status body headers] :as rsp}]
                    (t/is (string? body))
                    (t/is (= status 200))
                    (t/is (= (get headers "content-type") "application/json"))
                    (let [body (js->clj (.parse js/JSON body) :keywordize-keys true)]
                      (t/is (= {:foo "bar"} (:json body))))
                    (done))))))
