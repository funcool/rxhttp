# rxhttp

[![Clojars Project](http://clojars.org/funcool/rxhttp/latest-version.svg)](http://clojars.org/funcool/rxhttp)


## Introduction

A stream based http client for Clojure and ClojureScript that works in
browser, nodejs and JVM; and uses [beicon][1] as reactive streams
abstraction.

[1]: https://github.com/funcool/beicon


## Installation

Add the following dependency into your `project.clj` file:

```clojure
[funcool/rxhttp "1.0.0"]
```


## Getting started

This library just consists in one public namespace with one public method:

```clojure
(require '[rxhttp.core :as http]
         '[beicon.core :as rx])

(-> (http/send! {:method :get :url "https://httpbin.org/get"})
    (rx/subscribe (fn [{:keys [status headers body] :as response}]
                    (println "Response:" response))))
```

That's it, nothing more. You can see all available options in the
`send!` function docstring.

On JVM platform you also have a `send!!` function, that is a synchronous
variant of `send!`.


## How to contribute?

Just open an issue if you found an error or unexpected behavior or if
you want discuss some additional feature. If you found some unexpected
behavior and you have time to fix it, please open a PR!


## FAQ

- Why an other http client library?

Because I need and want to handle the response with reactive streams
and that abstraction allows easy way to abort/cancel a ingoing
request. A promise based solutions does allow that because promise
abstracion is not cancellable.

This library does not intend to reinvent the wheel, it just the well
established libraries under the hood and exposes an uniform api for
all platforms..

- Is this library a replacement for [httpurr][2]?

Yes. Reactive-Streams provide a better abstraction for represent an
asynchronous result; they are cancellable and lazy.


## License

_rxhttp_ is licensed under MPL-2.0 license.





