(ns liberator-mixin.validations.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [liberator-mixin.core :as core]
    [liberator-mixin.json.core :as json]
    [liberator-mixin.validation.core :as validation]
    [liberator-mixin.hypermedia.core :as hypermedia]
    [liberator-mixin.hal.core :as hal]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body json/wire-json->map)))

(deftype MockValidator [valid-response problems-for-response]
  validation/Validator
  (valid? [_ context]
    (when-not (:request context)
      (throw (ex-info "Not a valid context"
               {:context context})))
    valid-response)
  (problems-for [_ _] problems-for-response))

(defn new-mock-validator [valid-response problems-for-response]
  (->MockValidator valid-response problems-for-response))

(deftest validation-mixins
  (testing "with-validation"
    (testing "does nothing when the validator is not set"
      (let [resource (core/build-resource
                       (hal/with-hal-media-type)
                       (validation/with-validation)
                       {:handle-ok
                        (constantly {:status "OK"})})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept hal/hal-media-type))]
        (is (= 200 (:status response)))
        (is (= "OK"
              (get-in response [:body :status])))))

    (testing "does not validate request if not included in validate-methods"
      (let [resource (core/build-resource
                       (hal/with-hal-media-type)
                       (validation/with-validation)
                       {:allowed-methods  [:get :post]
                        :self             (constantly "https://example.com")
                        :validate-methods [:get]
                        :validator        (new-mock-validator false nil)}
                       {:handle-created (constantly {:status "OK"})})
            response (call-resource
                       resource
                       (->
                         (ring/request :post "/")
                         (ring/header :accept hal/hal-media-type)
                         (ring/header :content-type json/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 201 (:status response)))
        (is (= "OK"
              (get-in response [:body :status])))))

    (testing "validates incoming requests"
      (let [resource (core/build-resource
                       (hal/with-hal-media-type)
                       (hypermedia/with-self-link)
                       (validation/with-validation)
                       {:allowed-methods [:post]
                        :self            (constantly "https://example.com")
                        :validator       (new-mock-validator true nil)})
            response (call-resource
                       resource
                       (->
                         (ring/request :post "/")
                         (ring/header :accept hal/hal-media-type)
                         (ring/header :content-type json/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 201 (:status response)))))

    (testing "does not validate request if not included in validate-methods"
      (let [resource (core/build-resource
                       (hal/with-hal-media-type)
                       (hypermedia/with-self-link)
                       (validation/with-validation)
                       {:allowed-methods  [:get :post]
                        :self             (constantly "https://example.com")
                        :validate-methods [:get]
                        :validator        (new-mock-validator false nil)})
            response (call-resource
                       resource
                       (->
                         (ring/request :post "/")
                         (ring/header :accept hal/hal-media-type)
                         (ring/header :content-type json/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 201 (:status response)))))

    (testing "describes validation failures"
      (let [resource (core/build-resource
                       (hal/with-hal-media-type)
                       (hypermedia/with-self-link)
                       (validation/with-validation)
                       {:allowed-methods [:post]
                        :self            (constantly "https://example.com")
                        :validator       (new-mock-validator
                                           false
                                           [{:key "value"}])})
            response (call-resource
                       resource
                       (->
                         (ring/request :post "/")
                         (ring/header :accept hal/hal-media-type)
                         (ring/header :content-type json/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 422 (:status response)))
        (is (some? (get-in response [:body :error-id])))
        (is (= [{:key "value"}]
              (get-in response [:body :error-context])))))))
