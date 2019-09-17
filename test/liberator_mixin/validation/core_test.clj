(ns liberator-mixin.validation.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [jason.convenience :as jason-conv]

    [liberator-mixin.core :as core]
    [liberator-mixin.json.core :as json]
    [liberator-mixin.validation.core :as validation]))

(defn assert-valid-context [context]
  (when-not (:request context)
    (throw (ex-info "Not a valid context"
             {:context context}))))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(defn new-mock-validator [valid-response problems-for-response]
  (validation/validator
    :valid-fn
    (fn [context]
      (assert-valid-context context)
      valid-response)

    :problems-for-fn
    (fn [context]
      (assert-valid-context context)
      problems-for-response)))

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

  (testing "validates all known methods (except options) by default"
    (doseq [method #{:get :head :put :patch :spinach}]
      (let [resource
            (core/build-resource
              (json/with-json-media-type)
              (validation/with-validation)
              {:known-methods   [:get :head :put :patch :options :spinach]
               :allowed-methods [:get :head :put :patch :options :spinach]
               :validator       (new-mock-validator false nil)}
              {:handle-created (constantly {:status "OK"})
               :handle-ok      (constantly {:status "OK"})})
            response (call-resource
                       resource
                       (->
                         (ring/request method "/")
                         (ring/header :content-type json/json-media-type)
                         (ring/body {:key "value"})))]
        (is (= 422 (:status response))
          (str "Method: " method)))))

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
