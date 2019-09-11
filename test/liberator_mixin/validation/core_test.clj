(ns liberator-mixin.validation.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [jason.core :refer [defcoders]]

    [liberator-mixin.core :as core]
    [liberator-mixin.json.core :as json]
    [liberator-mixin.validation.core :as validation]))

(declare
  ->wire-json
  <-wire-json)

(defcoders wire)

(defn assert-valid-context [context]
  (when-not (:request context)
    (throw (ex-info "Not a valid context"
             {:context context}))))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body <-wire-json)))

(deftype MockValidator [valid-response problems-for-response]
  validation/Validator
  (valid? [_ context]
    (assert-valid-context context)
    valid-response)
  (problems-for [_ context]
    (assert-valid-context context)
    problems-for-response))

(defn new-mock-validator [valid-response problems-for-response]
  (->MockValidator valid-response problems-for-response))

(deftest with-validation
  (testing "does nothing when the validator is not set"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (validation/with-validation)
                     {:handle-ok
                      (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (ring/request :get "/"))]
      (is (= 200 (:status response)))
      (is (= "OK" (get-in response [:body :status])))))

  (testing "does not validate request if not included in validate-methods"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (validation/with-validation)
                     {:allowed-methods  [:get :post]
                      :validate-methods [:get]
                      :validator        (new-mock-validator false nil)}
                     {:handle-created (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (->
                       (ring/request :post "/")
                       (ring/header :content-type json/json-media-type)
                       (ring/body {:key "value"})))]
      (is (= 201 (:status response)))
      (is (= "OK" (get-in response [:body :status])))))

  (testing "validates incoming requests"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (validation/with-validation)
                     {:allowed-methods [:post]
                      :validator       (new-mock-validator true nil)}
                     {:handle-ok
                      (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (->
                       (ring/request :post "/")
                       (ring/header :content-type json/json-media-type)
                       (ring/body {:key "value"})))]
      (is (= 201 (:status response)))))

  (testing "describes validation failures"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (validation/with-validation)
                     {:allowed-methods [:post]
                      :validator       (new-mock-validator
                                         false
                                         [{:key "value"}])}
                     {:handle-ok
                      (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (->
                       (ring/request :post "/")
                       (ring/header :content-type json/json-media-type)
                       (ring/body {:key "value"})))]
      (is (= 422 (:status response)))
      (is (some? (get-in response [:body :error-id])))
      (is (= [{:key "value"}]
            (get-in response [:body :error-context]))))))
