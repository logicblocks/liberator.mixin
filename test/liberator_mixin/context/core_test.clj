(ns liberator-mixin.context.core-test
  (:require
    [clojure.test :refer :all]

    [jason.core :refer [defcoders]]

    [ring.mock.request :as ring]

    [liberator-mixin.core :as core]
    [liberator-mixin.context.core :as context]
    [liberator-mixin.json.core :as json]))

(declare
  ->wire-json
  <-wire-json)

(defcoders wire)

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body <-wire-json)))

(deftest with-dependency-in-context
  (testing "adds provided dependency to the context"
    (let [routes [["/" :root]]
          resource (core/build-resource
                     (json/with-json-media-type)
                     (context/with-attribute-in-context :routes routes)
                     {:handle-ok
                      (fn [{:keys [routes]}]
                        routes)})
          response (call-resource
                     resource
                     (ring/request :get "/"))]
      (is (= [["/" "root"]] (:body response))))))
