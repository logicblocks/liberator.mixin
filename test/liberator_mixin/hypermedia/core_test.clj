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

(deftest with-self-link
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
            (get-in response [:body :self]))))))
