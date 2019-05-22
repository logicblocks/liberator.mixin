(ns liberator-mixin.urls-test
  (:require [clojure.test :refer :all]
            [liberator-mixin.urls :refer :all]))

(deftest urls
  (testing "base-url"
    (testing "returns the domain name for a url"
      (is (= "https://example.com"
            (base-url {:scheme  :https
                       :headers {"host" "example.com"}})))

      (is (= "http://another.example.com"
            (base-url {:scheme  :http
                       :headers {"host" "another.example.com"}})))))

  (testing "absolute-url-for"
    (testing "returns the absolute url for a route"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     ["/examples" :examples]]]]
        (is (= "https://example.com/examples"
              (absolute-url-for request routes :examples)))))

    (testing "expands arguments"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     [["/examples/" :example-id] :example]]]]
        (is (= "https://example.com/examples/123"
              (absolute-url-for request routes :example
                :example-id 123))))))

  (testing "parameterised-url-for"
    (testing "describes a single parameter"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     ["/examples" :examples]]]]
        (is (= "https://example.com/examples{?first}"
              (parameterised-url-for request routes :examples
                [:first])))))

    (testing "describes multiple parameters"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     ["/examples" :examples]]]]
        (is (= "https://example.com/examples{?first,second}"
              (parameterised-url-for request routes :examples
                [:first :second])))))

    (testing "mixes positional and query parameters"
      (let [request {:scheme :https
                     :headers {"host" "example.com"}}
            routes [""
                    [["/" :root]
                     [["/examples/" :example-id] :example]]]]
        (is (= "https://example.com/examples/123{?first,second}"
              (parameterised-url-for request routes :example
                [:first :second]
                :example-id 123)))))))
