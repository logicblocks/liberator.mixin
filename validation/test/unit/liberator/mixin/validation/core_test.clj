(ns liberator.mixin.validation.core-test
  (:require
   [clojure.test :refer :all]
   [clojure.spec.alpha :as spec]

   [ring.mock.request :as ring]

   [jason.convenience :as jason-conv]

   [liberator.util :refer [by-method]]
   [liberator.mixin.core :as core]
   [liberator.mixin.json.core :as json]
   [liberator.mixin.validation.core :as validation]))

(defn assert-valid-context [context]
  (when-not (:request context)
    (throw (ex-info "Not a valid context"
             {:context context}))))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(defn new-mock-validator [valid-response problems-response]
  (validation/validator
    :valid?
    (fn [context]
      (assert-valid-context context)
      valid-response)

    :problems
    (fn [context]
      (assert-valid-context context)
      problems-response)))

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

  (testing "does not validate when no validator for method"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (validation/with-validation)
                     {:allowed-methods [:post :get]
                      :validator       (by-method
                                         :post (new-mock-validator false nil))}
                     {:handle-ok
                      (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (->
                       (ring/request :get "/")
                       (ring/header :content-type json/json-media-type)
                       (ring/body {:key "value"})))]
      (is (= 200 (:status response)))))

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

(deftest combine
  (testing "valid?"
    (testing "returns true when all validators return true"
      (let [validator1 (new-mock-validator true nil)
            validator2 (new-mock-validator true nil)
            validator3 (new-mock-validator true nil)
            combined-validator
            (validation/combine validator1 validator2 validator3)
            context {:request (ring/request :get "/")}]
        (is (= true (validation/valid? combined-validator context)))))

    (testing "returns false when any validator returns false"
      (let [validator1 (new-mock-validator true nil)
            validator2 (new-mock-validator false nil)
            combined-validator
            (validation/combine validator1 validator2)
            context {:request (ring/request :get "/")}]
        (is (= false (validation/valid? combined-validator context)))))

    (testing "returns false when all validators return false"
      (let [validator1 (new-mock-validator false nil)
            validator2 (new-mock-validator false nil)
            validator3 (new-mock-validator false nil)
            combined-validator
            (validation/combine validator1 validator2 validator3)
            context {:request (ring/request :get "/")}]
        (is (= false (validation/valid? combined-validator context))))))

  (testing "problems"
    (testing "concatenates problems from each validator when seqs"
      (let [validator1 (new-mock-validator false [{:first 1} {:second 2}])
            validator2 (new-mock-validator false '({:third 3} {:fourth 4}))
            validator3 (new-mock-validator false #{{:fifth 5}})
            combined-validator
            (validation/combine validator1 validator2 validator3)
            context {:request (ring/request :get "/")}]
        (is (= [{:first 1} {:second 2} {:third 3} {:fourth 4} {:fifth 5}]
              (validation/problems combined-validator context)))))

    (testing "concatenates problems from each validator when maps"
      (let [validator1 (new-mock-validator false {:first 1 :second 2})
            validator2 (new-mock-validator false {:third 3 :fourth 4})
            validator3 (new-mock-validator false {:fifth 5})
            combined-validator
            (validation/combine validator1 validator2 validator3)
            context {:request (ring/request :get "/")}]
        (is (= {:first  1
                :second 2
                :third  3
                :fourth 4
                :fifth  5}
              (validation/problems combined-validator context)))))))

(deftest function-backed-validator
  (testing "valid?"
    (testing "passes context to :valid?"
      (let [arg-store (atom nil)
            validator
            (validation/map->FnBackedValidator
              {:valid?   (fn [context] (reset! arg-store context))
               :problems (fn [_] [])})
            context {:first  1
                     :second 2}]
        (validation/valid? validator context)
        (is (= context @arg-store))))

    (testing "returns true when :valid? returns true"
      (let [validator
            (validation/map->FnBackedValidator
              {:valid?   (fn [_] true)
               :problems (fn [_] [])})]
        (is (= true (validation/valid? validator {})))))

    (testing "returns false when :valid? returns false"
      (let [validator
            (validation/map->FnBackedValidator
              {:valid?   (fn [_] false)
               :problems (fn [_] [])})]
        (is (= false (validation/valid? validator {}))))))

  (testing "problems"
    (testing "passes context to :problems"
      (let [arg-store (atom nil)
            validator
            (validation/map->FnBackedValidator
              {:valid?   (fn [_] false)
               :problems (fn [context] (reset! arg-store context))})
            context {:first  1
                     :second 2}]
        (validation/problems validator context)
        (is (= context @arg-store))))

    (testing "returns problems returned by :problems"
      (let [validator
            (validation/map->FnBackedValidator
              {:valid?   (fn [_] true)
               :problems (fn [_] [{:first 1} {:second 2}])})]
        (is (= [{:first 1} {:second 2}]
              (validation/problems validator {})))))))

(spec/def ::test-attribute-1 string?)
(spec/def ::test-attribute-2 int?)
(spec/def ::test-spec
  (spec/keys
    :req [::test-attribute-1 ::test-attribute-2]))

(deftest spec-backed-validator
  (testing "valid?"
    (testing "returns true when spec is satisfied by context"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec ::test-spec})]
        (is (= true (validation/valid? validator
                      {::test-attribute-1 "hello"
                       ::test-attribute-2 123})))))

    (testing "returns false when spec is not satisfied by context"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec ::test-spec})]
        (is (= false (validation/valid? validator
                       {::test-attribute-1 123})))))

    (testing "uses specified selector when provided"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec     ::test-spec
               :selector first})]
        (is (= true (validation/valid? validator
                      [{::test-attribute-1 "hello"
                        ::test-attribute-2 123}]))))))

  (testing "problems"
    (testing "returns no problems when spec is satisfied by context"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec ::test-spec})]
        (is (= [] (validation/problems validator
                    {::test-attribute-1 "hello"
                     ::test-attribute-2 123})))))

    (testing "returns problems when spec is not satisfied by context"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec ::test-spec})]
        (is (= [{:field
                 [:liberator.mixin.validation.core-test/test-attribute-2]
                 :requirements [:must-be-present]
                 :subject      :test-spec
                 :type         :missing}
                {:field
                 [:liberator.mixin.validation.core-test/test-attribute-1]
                 :requirements [:must-be-valid]
                 :subject      :test-spec
                 :type         :invalid}]
              (validation/problems validator
                {::test-attribute-1 123})))))

    (testing "uses specified selector when provided"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec     ::test-spec
               :selector first})]
        (is (= [{:field
                 [:liberator.mixin.validation.core-test/test-attribute-2]
                 :requirements [:must-be-present]
                 :subject      :test-spec
                 :type         :missing}]
              (validation/problems validator
                [{::test-attribute-1 "hello"}])))))

    (testing "uses specified transformer when provided"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec ::test-spec
               :problem-transformer
               (fn [{:keys [subject type field]}]
                 {:type    :validation-failure
                  :subject subject
                  :problem type
                  :field   (name (first field))})})]
        (is (= [{:type    :validation-failure
                 :subject :test-spec
                 :problem :missing
                 :field   "test-attribute-2"}
                {:type    :validation-failure
                 :subject :test-spec
                 :problem :invalid
                 :field   "test-attribute-1"}]
              (validation/problems validator
                {::test-attribute-1 123})))))
    (testing "uses specified subject function when provided"
      (let [validator
            (validation/map->SpecBackedValidator
              {:spec ::test-spec
               :problem-subject
               (fn [spec value]
                 (str (name spec) "-" (::test-attribute-1 value)))})]
        (is (= [{:field
                 [:liberator.mixin.validation.core-test/test-attribute-2]
                 :requirements [:must-be-present]
                 :subject      "test-spec-123"
                 :type         :missing}
                {:field
                 [:liberator.mixin.validation.core-test/test-attribute-1]
                 :requirements [:must-be-valid]
                 :subject      "test-spec-123"
                 :type         :invalid}]
              (validation/problems validator
                {::test-attribute-1 123})))))))

(deftest validator
  (testing "assumes function backed validator when no type is passed"
    (let [valid (constantly true)
          problems (constantly [])]
      (is (= (validation/map->FnBackedValidator
               {:valid?   valid
                :problems problems})
            (validation/validator
              :valid? valid
              :problems problems)))))

  (testing "returns spec backed validator when type is spec"
    (let [selector #(get-in % [:some :thing])]
      (is (= (validation/map->SpecBackedValidator
               {:spec     ::test-spec
                :selector selector})
            (validation/validator
              :type :spec
              :spec ::test-spec
              :selector selector)))))

  (testing "returns function backed validator when type is fn"
    (let [valid (constantly true)
          problems (constantly [])]
      (is (= (validation/map->FnBackedValidator
               {:valid?   valid
                :problems problems})
            (validation/validator
              :type :fn
              :valid? valid
              :problems problems))))))

(deftest spec-validator
  (testing "returns spec backed validator"
    (is (= (validation/map->SpecBackedValidator
             {:spec ::test-spec})
          (validation/spec-validator ::test-spec))))
  (testing "passes selector when creating spec backed validator"
    (let [selector #(get-in % [:some :thing])]
      (is (= (validation/map->SpecBackedValidator
               {:spec     ::test-spec
                :selector selector})
            (validation/spec-validator ::test-spec
              {:selector selector})))))
  (testing "passes problem-transformer when creating spec backed validator"
    (let [problem-transformer
          (fn [{:keys [subject type field]}]
            {:type    :validation-failure
             :subject subject
             :problem type
             :field   (name (first field))})]
      (is (= (validation/map->SpecBackedValidator
               {:spec                   ::test-spec
                :problem-transformer problem-transformer})
            (validation/spec-validator ::test-spec
              {:problem-transformer problem-transformer})))))
  (testing "passes problem-subject when creating spec backed validator"
    (let [problem-subject
          (fn [spec value]
            (str (name spec) "-" (:id value)))]
      (is (= (validation/map->SpecBackedValidator
               {:spec               ::test-spec
                :problem-subject problem-subject})
            (validation/spec-validator ::test-spec
              {:problem-subject problem-subject}))))))
