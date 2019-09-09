(ns liberator-mixin.hypermedia.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [liberator-mixin.core :as core]
    [liberator-mixin.json.core :as json]
    [liberator-mixin.hypermedia.core :as hypermedia]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body json/wire-json->map)))

(deftest hypermedia-mixins
  (testing "with-routes-in-context"
    (testing "adds routes to the context"
      (let [routes [["/" :root]]
            resource (core/build-resource
                       (hypermedia/with-routes-in-context routes)
                       {:handle-ok
                        (fn [{:keys [routes]}]
                          routes)})
            response (call-resource
                       resource
                       (ring/request :get "/"))]
        (is (some? (:body response))))))

  (testing "with-self-link"
    (testing "adds a self link to context"
      (let [self-link "https://self.example.com"
            resource (core/build-resource
                       (json/with-json-media-type)
                       (hypermedia/with-self-link)
                       {:self (constantly self-link)
                        :handle-ok
                        (fn [{:keys [self]}]
                          {:self self})})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept json/json-media-type))]
        (is (=
              self-link
              (get-in response [:body :self])))))))

(deftest urls
  (testing "base-url"
    (testing "returns the domain name for a url"
      (is (= "https://example.com"
            (hypermedia/base-url {:scheme  :https
                                  :headers {"host" "example.com"}})))

      (is (= "http://another.example.com"
            (hypermedia/base-url {:scheme  :http
                                  :headers {"host" "another.example.com"}})))))

  (testing "absolute-url-for"
    (testing "returns the absolute url for a route"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     ["/examples" :examples]]]]
        (is (= "https://example.com/examples"
              (hypermedia/absolute-url-for request routes :examples)))))

    (testing "expands arguments"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     [["/examples/" :example-id] :example]]]]
        (is (= "https://example.com/examples/123"
              (hypermedia/absolute-url-for request routes :example
                :example-id 123))))))

  (testing "parameterised-url-for"
    (testing "describes a single parameter"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     ["/examples" :examples]]]]
        (is (= "https://example.com/examples{?first}"
              (hypermedia/parameterised-url-for request routes :examples
                [:first])))))

    (testing "describes multiple parameters"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     ["/examples" :examples]]]]
        (is (= "https://example.com/examples{?first,second}"
              (hypermedia/parameterised-url-for request routes :examples
                [:first :second])))))

    (testing "mixes positional and query parameters"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     [["/examples/" :example-id] :example]]]]
        (is (= "https://example.com/examples/123{?first,second}"
              (hypermedia/parameterised-url-for request routes :example
                [:first :second]
                :example-id 123)))))))
