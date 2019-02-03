(ns com.b-social.microservice-tools.resources.validation-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as ring]
            [com.b-social.microservice-tools.json :as json]
            [com.b-social.microservice-tools.validation :as validation]
            [com.b-social.microservice-tools.liberator :as l]
            [com.b-social.microservice-tools.resources.hypermedia :as r]
            [com.b-social.microservice-tools.resources.validation :as v]
            [com.b-social.microservice-tools.resources.json :as j]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body json/wire-json->map)))

(deftype MockValidator [valid-response problems-for-response]
  validation/Validator
  (valid? [_ _] valid-response)
  (problems-for [_ _] problems-for-response))

(defn new-mock-validator [valid-response problems-for-response]
  (->MockValidator valid-response problems-for-response))

(deftest validation-mixins
  (testing "with-validation"
    (testing "does nothing when the validator is not set"
      (let [resource (l/build-resource
                       (r/with-hal-media-type)
                       (v/with-validation)
                       {:handle-ok
                        (constantly {:status "OK"})})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept r/hal-media-type))]
        (is (= 200 (:status response)))
        (is (= "OK"
              (get-in response [:body :status])))))

    (testing "validates incoming requests"
      (let [resource (l/build-resource
                       (r/with-hal-media-type)
                       (r/with-self-link)
                       (v/with-validation)
                       {:allowed-methods [:post]
                        :self            (constantly "https://example.com")
                        :validator       (new-mock-validator true nil)})
            response (call-resource
                       resource
                       (->
                         (ring/request :post "/")
                         (ring/header :accept r/hal-media-type)
                         (ring/header :content-type j/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 201 (:status response)))))

    (testing "describes validation failures"
      (let [resource (l/build-resource
                       (r/with-hal-media-type)
                       (r/with-self-link)
                       (v/with-validation)
                       {:allowed-methods [:post]
                        :self            (constantly "https://example.com")
                        :validator       (new-mock-validator
                                           false
                                           [{:key "value"}])})
            response (call-resource
                       resource
                       (->
                         (ring/request :post "/")
                         (ring/header :accept r/hal-media-type)
                         (ring/header :content-type j/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 422 (:status response)))
        (is (some? (get-in response [:body :error-id])))
        (is (= [{:key "value"}]
              (get-in response [:body :error-context])))))))
