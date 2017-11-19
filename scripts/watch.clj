(require '[cljs.build.api :as b])

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

(b/watch (b/inputs "test" "src") options)
