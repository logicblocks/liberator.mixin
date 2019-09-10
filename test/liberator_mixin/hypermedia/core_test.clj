(ns liberator-mixin.hypermedia.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [jason.core :refer [defcoders]]

    [liberator-mixin.core :as core]
    [liberator-mixin.json.core :as json]
    [liberator-mixin.hypermedia.core :as hypermedia]))

(declare <-wire-json)
(defcoders wire)

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body <-wire-json)))

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
