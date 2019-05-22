(ns liberator-mixin.json.core-test
  (:require
    [clojure.test :refer :all]

    [clojure.string :as str]
    [clj-time.core :as date]

    [ring.mock.request :as ring]

    [liberator-mixin.core :as l]
    [liberator-mixin.json.core :as json]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body json/wire-json->map)))

(deftest json-mixins
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
                      (ring/body (json/map->wire-json {:key "value"})))
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

(defn- long-str [& args]
  (str/join "\n" args))

(deftest json
  (testing "wire-json->map"
    (testing "parses json"
      (is (=
            {:key 123}
            (json/wire-json->map "{\"key\": 123}"))))

    (testing "converts keys to kebab case"
      (is (=
            {:some-key 123}
            (json/wire-json->map "{\"someKey\": 123}"))))

    (testing "preserves keys prefixed with an underscore"
      (is (=
            {:_someLinks 123}
            (json/wire-json->map "{\"_someLinks\": 123}")))))

  (testing "map->wire-json"
    (testing "returns a json string"
      (is (=
            (long-str
              "{"
              "  \"key\" : 123"
              "}")
            (json/map->wire-json {:key 123}))))

    (testing "converts dates"
      (is (=
            (long-str
              "{"
              "  \"key\" : \"2019-02-03T00:00:00.000Z\""
              "}")
            (json/map->wire-json {:key (date/date-time 2019 2 3)}))))

    (testing "converts keys to kebab case"
      (is (=
            (long-str
              "{"
              "  \"someKey\" : 123"
              "}")
            (json/map->wire-json {:some-key 123}))))

    (testing "preserves meta keys"
      (is (=
            (long-str
              "{"
              "  \"_some-key\" : 123"
              "}")
            (json/map->wire-json {:_some-key 123})))))

  (testing "db-json->map"
    (testing "parses json"
      (is (=
            {:key 123}
            (json/db-json->map "{\"key\": 123}"))))

    (testing "converts keys to kebab case"
      (is (=
            {:some-key 123}
            (json/db-json->map "{\"some_key\": 123}"))))

    (testing "preserves keys prefixed with an underscore"
      (is (=
            {:_some_links 123}
            (json/db-json->map "{\"_some_links\": 123}")))))

  (testing "map->db-json"
    (testing "returns a json string"
      (is (=
            (long-str
              "{"
              "  \"key\" : 123"
              "}")
            (json/map->db-json {:key 123}))))

    (testing "converts dates"
      (is (=
            (long-str
              "{"
              "  \"key\" : \"2019-02-03T00:00:00.000Z\""
              "}")
            (json/map->db-json {:key (date/date-time 2019 2 3)}))))

    (testing "converts keys to snake case"
      (is (=
            (long-str
              "{"
              "  \"some_key\" : 123"
              "}")
            (json/map->db-json {:some-key 123}))))

    (testing "preserves meta keys"
      (is (=
            (long-str
              "{"
              "  \"_some-key\" : 123"
              "}")
            (json/map->db-json {:_some-key 123}))))))
