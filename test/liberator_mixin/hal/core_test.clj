(ns liberator-mixin.hal.core-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]

    [jason.core :as jason]
    [jason.convenience :as jason-conv]

    [camel-snake-kebab.core :refer [->snake_case_string]]

    [liberator-mixin.core :as core]
    [liberator-mixin.logging.core :as log]
    [liberator-mixin.json.core :as json]
    [liberator-mixin.hypermedia.core :as hypermedia]
    [liberator-mixin.hal.core :as hal]))

(deftype TestLogger [state]
  log/Logger
  (log-error [_ message context cause]
    (reset!
      state
      {:message message
       :context context
       :cause   cause})))

(defn new-test-logger [state]
  (->TestLogger state))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(deftest default-json-encoding
  (testing "produces camel case meta preserving JSON by default"
    (testing "for maps"
      (let [resource (core/build-resource
                       (hypermedia/with-hypermedia-mixin)
                       (json/with-json-mixin)
                       (hal/with-hal-media-type)
                       {:handle-ok
                        (constantly {:_meta-data  {:some-field "thing1"}
                                     :other-field "thing2"})})
            response (resource
                       (-> (ring/request :get "/")
                         (ring/header "Accept" "application/hal+json")))]
        (is (= (str
                 "{\n"
                 "  \"_metaData\" : {\n"
                 "    \"someField\" : \"thing1\"\n"
                 "  },\n"
                 "  \"otherField\" : \"thing2\"\n"
                 "}")
              (:body response)))))

    (testing "for seqs"
      (let [resource (core/build-resource
                       (hypermedia/with-hypermedia-mixin)
                       (json/with-json-mixin)
                       (hal/with-hal-media-type)
                       {:handle-ok
                        (constantly [{:some-key 1} {:_meta-data 2}])})
            response (resource
                       (-> (ring/request :get "/")
                         (ring/header "Accept" "application/hal+json")))]
        (is (= (str
                 "[ {\n"
                 "  \"someKey\" : 1\n"
                 "}, {\n"
                 "  \"_metaData\" : 2\n"
                 "} ]")
              (:body response)))))))

(deftest custom-json-encoding
  (testing "uses underlying JSON mixin encoder"
    (testing "for maps"
      (let [encoder (jason/new-json-encoder
                      (jason/new-object-mapper
                        {:pretty true
                         :encode-key-fn
                         (jason/->encode-key-fn ->snake_case_string)}))
            resource (core/build-resource
                       (hypermedia/with-hypermedia-mixin)
                       (json/with-json-mixin
                         {:json {:encoder encoder}})
                       (hal/with-hal-media-type)
                       {:handle-ok
                        (constantly {:_meta-data  {:some-field "thing1"}
                                     :other-field "thing2"})})
            response (resource
                       (-> (ring/request :get "/")
                         (ring/header "Accept" "application/hal+json")))]
        (is (= (str
                 "{\n"
                 "  \"_meta_data\" : {\n"
                 "    \"some_field\" : \"thing1\"\n"
                 "  },\n"
                 "  \"other_field\" : \"thing2\"\n"
                 "}")
              (:body response)))))

    (testing "for seqs"
      (let [encoder (jason/new-json-encoder
                      (jason/new-object-mapper
                        {:pretty true
                         :encode-key-fn
                         (jason/->encode-key-fn ->snake_case_string)}))
            resource (core/build-resource
                       (hypermedia/with-hypermedia-mixin)
                       (json/with-json-mixin
                         {:json {:encoder encoder}})
                       (hal/with-hal-media-type)
                       {:handle-ok
                        (constantly [{:some-key 1} {:_meta-data 2}])})
            response (resource
                       (-> (ring/request :get "/")
                         (ring/header "Accept" "application/hal+json")))]
        (is (= (str
                 "[ {\n"
                 "  \"some_key\" : 1\n"
                 "}, {\n"
                 "  \"_meta_data\" : 2\n"
                 "} ]")
              (:body response)))))))

(deftest with-hal-media-type
  (testing "allows hypermedia requests"
    (let [resource (core/build-resource
                     (hypermedia/with-hypermedia-mixin)
                     (json/with-json-mixin)
                     (hal/with-hal-media-type)
                     {:handle-ok (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/")
                       :accept hal/hal-media-type))]
      (is (= 200 (:status response)))
      (is (= {:status "OK"}
            (:body response)))))

  (testing "correctly responds when unauthorised"
    (let [resource (core/build-resource
                     (hypermedia/with-hypermedia-mixin)
                     (json/with-json-mixin)
                     (hal/with-hal-media-type)
                     {:authorized?         false
                      :handle-unauthorized {:error "unauthorised"}})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/")
                       :accept hal/hal-media-type))]
      (is (= 401 (:status response)))
      (is (= {:error "unauthorised"}
            (:body response))))))

(deftest with-not-found-handler
  (testing "provides a sensible default when the resource does not exist"
    (let [resource (core/build-resource
                     (hypermedia/with-hypermedia-mixin)
                     (json/with-json-mixin)
                     (hal/with-hal-media-type)
                     (hal/with-not-found-handler)
                     {:exists? (constantly false)})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/")
                       :accept hal/hal-media-type))]
      (is (= 404 (:status response)))
      (is (= "Resource not found"
            (get-in response [:body :error]))))))

(deftest with-exception-handler
  (let [log-state-atom (atom {})
        resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin)
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-exception-handler)
                   {:logger
                    (constantly (new-test-logger log-state-atom))
                    :handle-ok
                    #(throw (ex-info "Something went wrong" {}))})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :get "/")
                     :accept hal/hal-media-type))]
    (testing "returns a json response when an exception is thrown"
      (is (= 500 (:status response)))
      (is (some? (get-in response [:body :error-id])))
      (is (= "Request caused an exception"
            (get-in response [:body :message]))))

    (testing "calls the logger when present"
      (let [log-state @log-state-atom]
        (is (= "Request caused an exception"
              (:message log-state)))
        (is (some? (:context log-state)))
        (is (some? (:cause log-state)))))))

(deftest with-unauthorized-handler
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin)
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-unauthorized-handler)
                   {:authorized? (constantly false)})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :get "/")
                     :accept hal/hal-media-type))]
    (testing "returns a json response when request is unauthorized"
      (is (= 401 (:status response)))
      (is (= "Unauthorized"
            (get-in response [:body :error]))))))

(deftest with-forbidden-handler
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin)
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-forbidden-handler)
                   {:allowed? (constantly false)})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :get "/")
                     :accept hal/hal-media-type))]
    (testing "returns a json response when request is forbidden"
      (is (= 403 (:status response)))
      (is (= "Forbidden"
            (get-in response [:body :error]))))))

(deftest with-method-not-allowed-handler
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin)
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-method-not-allowed-handler)
                   {:allowed-methods [:get]})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :post "/" {})
                     :accept hal/hal-media-type))]
    (testing "returns a json response when request method is not allowed"
      (is (= 405 (:status response)))
      (is (= "Method not allowed"
            (get-in response [:body :error]))))))
