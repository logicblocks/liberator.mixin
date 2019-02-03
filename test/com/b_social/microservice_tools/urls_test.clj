(ns com.b-social.microservice-tools.urls-test
  (:require [clojure.test :refer :all]
            [com.b-social.microservice-tools.urls :refer :all]))

(deftest urls
  (testing "base-url"
    (testing "returns the domain name for a url"
      (is (= "https://example.com"
            (base-url {:scheme  :https
                       :headers {"host" "example.com"}})))

      (is (= "http://another.example.com"
            (base-url {:scheme  :http
                       :headers {"host" "another.example.com"}}))))))
