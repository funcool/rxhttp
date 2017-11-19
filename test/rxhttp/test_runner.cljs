(ns rxhttp.test-runner
  (:require [cljs.test :as test]
            [rxhttp.test-nodejs]
            [rxhttp.test-browser]))

(enable-console-print!)

(defmethod test/report [:cljs.test/default :end-run-tests]
  [m]
  (if (test/successful? m)
    (.exit js/process) 0)
    (.exit js/process) 1)

(defn main
  []
  (test/run-tests (test/empty-env)
                  'rxhttp.test-browser
                  'rxhttp.test-nodejs))

(set! *main-cli-fn* main)
