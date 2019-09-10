(ns liberator-mixin.json.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [jason.core :refer [defcoders]]

    [liberator-mixin.core :as l]
    [liberator-mixin.json.core :as json]))

(declare
  ->wire-json
  <-wire-json)

(defcoders wire)

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body <-wire-json)))

(deftest json-mixin
  (testing "with-json-media-type"
    (testing "allows hypermedia requests"
      (let [resource (l/build-resource
                       (json/with-json-media-type)
                       {:handle-ok (constantly {:status "OK"})})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept json/json-media-type))]
        (is (= 200 (:status response)))
        (is (= {:status "OK"}
              (:body response))))))

  (testing "with-body-parsed-as-json"
    (testing "parses the body as json"
      (let [resource (l/build-resource
                       (json/with-json-media-type)
                       (json/with-body-parsed-as-json)
                       {:allowed-methods [:post]
                        :handle-created
                        (fn [{:keys [request]}]
                          (:body request))})
            request (->
                      (ring/request :post "/")
                      (ring/header "Accept" json/json-media-type)
                      (ring/header "Content-Type" json/json-media-type)
                      (ring/body (->wire-json {:key "value"})))
            response (call-resource resource request)]
        (is (= 201 (:status response)))
        (is (=
              {:key "value"}
              (:body response)))))
    (testing "parses the body as json"
      (let [resource (l/build-resource
                       (json/with-json-media-type)
                       (json/with-body-parsed-as-json)
                       {:allowed-methods [:post]
                        :handle-created
                        (fn [{:keys [request]}]
                          (:body request))})
            request (->
                      (ring/request :post "/")
                      (ring/header "Accept" json/json-media-type)
                      (ring/header "Content-Type" json/json-media-type)
                      (ring/body (->wire-json {:key "value"})))
            response (call-resource resource request)]
        (is (= 201 (:status response)))
        (is (=
              {:key "value"}
              (:body response)))))

    (testing "returns a malformed status when it is not valid json"
      (let [resource (l/build-resource
                       (json/with-json-media-type)
                       (json/with-body-parsed-as-json)
                       {:allowed-methods [:post]})
            request (->
                      (ring/request :post "/")
                      (ring/header "Accept" json/json-media-type)
                      (ring/header "Content-Type" json/json-media-type)
                      (ring/body "not valid json"))
            response (resource request)]
        (is (= 400 (:status response)))))))
