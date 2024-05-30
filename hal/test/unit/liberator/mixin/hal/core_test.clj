(ns liberator.mixin.hal.core-test
  (:require
   [clojure.test :refer :all]

   [cartus.test :refer [logged?] :as ct]

   [ring.mock.request :as ring]

   [jason.core :as jason]
   [jason.convenience :as jason-conv]

   [camel-snake-kebab.core :refer [->snake_case_string]]

   [liberator.mixin.core :as core]
   [liberator.mixin.json.core :as json]
   [liberator.mixin.hypermedia.core :as hypermedia]
   [liberator.mixin.hal.core :as hal]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(deftest json-encodes-as-camel-case-meta-preserving-JSON-for-maps-by-default
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

(deftest json-encodes-as-camel-case-meta-preserving-JSON-for-seqs-by-default
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
          (:body response)))))

(deftest json-encodes-using-specified-encoder-for-maps-when-provided
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

(deftest json-encodes-using-specified-encoder-for-seqs-when-provided
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
          (:body response)))))

(deftest with-hal-media-type-allows-hal-request
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

(deftest with-hal-media-type-responds-correctly-on-error
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
          (:body response)))))

(deftest with-not-found-handler-responds-404-with-message-when-not-found
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin
                     {:router ["" [["/" :discovery]]]})
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
          (get-in response [:body :error])))))

(deftest with-exception-handler-responds-500-with-obscured-error-on-exception
  (let [logger (ct/logger)
        resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin
                     {:router ["" [["/" :discovery]]]})
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-exception-handler)
                   {:logger (constantly logger)
                    :handle-ok
                    #(throw (ex-info "Something went wrong" {}))})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :get "/")
                     :accept hal/hal-media-type))]
    (is (= 500 (:status response)))
    (is (some? (get-in response [:body :error-id])))
    (is (= "Request caused an exception"
          (get-in response [:body :message])))))

(deftest with-exception-handler-logs-when-logger-on-resource
  (let [logger (ct/logger)
        exception (ex-info "Something went wrong" {:some :data})
        resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin
                     {:router ["" [["/" :discovery]]]})
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-exception-handler)
                   {:logger    (constantly logger)
                    :handle-ok (fn [_] (throw exception))})]

    (call-resource
      resource
      (ring/header
        (ring/request :get "/")
        :accept hal/hal-media-type))

    (is (logged? logger
          {:level     :error
           :type      :service.rest/request.exception.unhandled
           :context   {:error-id some?
                       :exception (Throwable->map exception)}}))))

(deftest with-unauthorized-handler-responds-401-unauthorized-when-unauthorized
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin
                     {:router ["" [["/" :discovery]]]})
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-unauthorized-handler)
                   {:authorized? (constantly false)})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :get "/")
                     :accept hal/hal-media-type))]
    (is (= 401 (:status response)))
    (is (= "Unauthorized"
          (get-in response [:body :error])))))

(deftest with-forbidden-handler-responds-403-forbidden-when-forbidden
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin
                     {:router ["" [["/" :discovery]]]})
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-forbidden-handler)
                   {:allowed? (constantly false)})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :get "/")
                     :accept hal/hal-media-type))]
    (is (= 403 (:status response)))
    (is (= "Forbidden"
          (get-in response [:body :error])))))

(deftest with-method-not-allowed-handler-responds-405-not-allows-when-bad-method
  (let [resource (core/build-resource
                   (hypermedia/with-hypermedia-mixin
                     {:router ["" [["/" :discovery]]]})
                   (json/with-json-mixin)
                   (hal/with-hal-media-type)
                   (hal/with-method-not-allowed-handler)
                   {:allowed-methods [:get]})
        response (call-resource
                   resource
                   (ring/header
                     (ring/request :post "/" {})
                     :accept hal/hal-media-type))]
    (is (= 405 (:status response)))
    (is (= "Method not allowed"
          (get-in response [:body :error])))))

(deftest with-malformed-handler-responds-400-not-allows-when-bad-method
  (testing "Default message"
    (let [resource (core/build-resource
                     (hypermedia/with-hypermedia-mixin
                       {:router ["" [["/" :discovery]]]})
                     (json/with-json-mixin)
                     (hal/with-hal-media-type)
                     (hal/with-malformed-handler)
                     {:malformed? (constantly true)
                      :handle-ok (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/" {})
                       :accept hal/hal-media-type))]
      (is (= 400 (:status response)))
      (is (= "Malformed"
            (get-in response [:body :error])))))
  (testing "Custom malformed message"
    (let [resource (core/build-resource
                     (hypermedia/with-hypermedia-mixin
                       {:router ["" [["/" :discovery]]]})
                     (json/with-json-mixin)
                     (hal/with-hal-media-type)
                     (hal/with-malformed-handler)
                     {:malformed? (constantly [true {:malformed-message "OH NO"}])
                      :handle-ok (constantly {:status "OK"})})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/" {})
                       :accept hal/hal-media-type))]
      (is (= 400 (:status response)))
      (is (= "OH NO"
            (get-in response [:body :error]))))))
