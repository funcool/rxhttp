(require '[cljs.build.api :as b])

(println "Building ...")

(def options
  {:main 'rxhttp.test-runner
   :output-to "out/tests.js"
   :output-dir "out"
   :target :nodejs
   :optimizations :none
   :pretty-print false
   :language-in  :ecmascript5
   :language-out :ecmascript5
   :verbose true})

(let [start (System/nanoTime)]
  (b/build (b/inputs "test" "src") options)
  (println "... done. Elapsed" (/ (- (System/nanoTime) start) 1e9) "seconds"))
