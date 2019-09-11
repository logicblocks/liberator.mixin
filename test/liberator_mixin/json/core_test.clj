(ns liberator-mixin.json.core-test
  (:require
    [clojure.test :refer :all]

    [camel-snake-kebab.core
     :refer [->snake_case_keyword
             ->snake_case_string]]

    [ring.mock.request :as ring]
    [ring.middleware.params :as params]
    [ring.middleware.keyword-params :as keyword-params]

    [jason.core :as jason :refer [defcoders]]

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

(deftest default-json-encoding
  (testing "produces camel case meta preserving JSON by default"
    (testing "for maps"
      (let [resource (l/build-resource
                       (json/with-json-media-type)
                       {:handle-ok
                        (constantly {:_meta-data  {:some-field "thing1"}
                                     :other-field "thing2"})})
            response (resource (ring/request :get "/"))]
        (is (= (str
                 "{\n"
                 "  \"_metaData\" : {\n"
                 "    \"someField\" : \"thing1\"\n"
                 "  },\n"
                 "  \"otherField\" : \"thing2\"\n"
                 "}")
              (:body response)))))

    (testing "for seqs"
      (let [resource (l/build-resource
                       (json/with-json-media-type)
                       {:handle-ok
                        (constantly [{:some-key 1} {:_meta-data 2}])})
            response (resource (ring/request :get "/"))]
        (is (= (str
                 "[ {\n"
                 "  \"someKey\" : 1\n"
                 "}, {\n"
                 "  \"_metaData\" : 2\n"
                 "} ]")
              (:body response)))))))

(deftest with-json-media-type
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

(deftest with-body-parsed-as-json
  (testing "parses the body as JSON"
    (let [resource (l/build-resource
                     (json/with-json-media-type)
                     (json/with-body-parsed-as-json)
                     {:allowed-methods
                      [:post]

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
            (:body response))))))

(deftest with-params-parsed-as-json
  (testing "parses simple JSON params"
    (let [resource
          (->
            (l/build-resource
              (json/with-json-media-type)
              (json/with-params-parsed-as-json)
              {:allowed-methods
               [:get]

               :handle-ok
               (fn [{:keys [request]}]
                 (let [params (:params request)]
                   {:correct-params
                    (and
                      (= (:thing1 params) [1 2 3])
                      (= (:thing2 params)
                        {:key-1 "value"}))}))})
            (keyword-params/wrap-keyword-params)
            (params/wrap-params))
          request (->
                    (ring/request :get "/")
                    (ring/header "Content-Type" json/json-media-type)
                    (ring/query-string
                      "thing1=[1,2,3]&thing2={\"key1\":\"value\"}"))
          response (call-resource resource request)]
      (is (= 200 (:status response)))
      (is (= {:correct-params true}
            (:body response))))))

(deftest with-json-decoder
  (testing "overrides default JSON decoder"
    (let [decoder (jason/new-json-decoder
                    (jason/new-object-mapper
                      {:decode-key-fn
                       (jason/->decode-key-fn ->snake_case_keyword)}))
          resource (l/build-resource
                     (json/with-json-decoder decoder)
                     (json/with-json-media-type)
                     (json/with-body-parsed-as-json)
                     {:allowed-methods
                      [:post]

                      :handle-created
                      (fn [{:keys [request]}]
                        (if (= (:body request) {:some_key "value"})
                          {:received-correct-body true}
                          {:received-correct-body false}))})
          request (->
                    (ring/request :post "/")
                    (ring/header "Accept" json/json-media-type)
                    (ring/header "Content-Type" json/json-media-type)
                    (ring/body (->wire-json {:some-key "value"})))
          response (call-resource resource request)]
      (is (= 201 (:status response)))
      (is (= {:received-correct-body true}
            (:body response))))))

(deftest with-json-encoder
  (testing "overrides default JSON encoder"
    (let [encoder (jason/new-json-encoder
                    (jason/new-object-mapper
                      {:encode-key-fn
                       (jason/->decode-key-fn ->snake_case_string)
                       :pretty false}))
          resource (l/build-resource
                     (json/with-json-encoder encoder)
                     (json/with-json-media-type)
                     (json/with-body-parsed-as-json)
                     {:allowed-methods
                      [:post]

                      :handle-created
                      (fn [_]
                        {:some-key "value"})})
          request (->
                    (ring/request :post "/")
                    (ring/header "Accept" json/json-media-type)
                    (ring/header "Content-Type" json/json-media-type)
                    (ring/body (->wire-json {:request "value"})))
          response (resource request)]
      (is (= 201 (:status response)))
      (is (= "{\"some_key\":\"value\"}"
            (:body response))))))

(deftest with-json-mixin
  (testing "includes media type, parsed body and encoder and decoder"
    (let [{:keys [->json <-json]}
          (jason/new-json-coders
            {:encode-key-fn (jason/->encode-key-fn ->snake_case_string)
             :decode-key-fn (jason/->decode-key-fn ->snake_case_keyword)})
          resource (l/build-resource
                     (json/with-json-mixin
                       {:json {:encoder ->json
                               :decoder <-json}})
                     {:allowed-methods
                      [:post]

                      :handle-created
                      (fn [{:keys [request]}]
                        {:correct-request
                         (= (:body request) {:some_key "value"})})})
          request (->
                    (ring/request :post "/")
                    (ring/header "Accept" json/json-media-type)
                    (ring/header "Content-Type" json/json-media-type)
                    (ring/body (->json {:some-key "value"})))
          response (resource request)]
      (is (= 201 (:status response)))
      (is (= (str
               "{\n"
               "  \"correct_request\" : true\n"
               "}")
            (:body response))))))
