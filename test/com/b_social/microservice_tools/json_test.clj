(ns com.b-social.microservice-tools.json-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-time.core :as date]
            [com.b-social.microservice-tools.json :refer :all]))

(defn- long-str [& args]
  (str/join "\n" args))

(deftest json
  (testing "wire-json->map"
    (testing "parses json"
      (is (=
            {:key 123}
            (wire-json->map "{\"key\": 123}"))))

    (testing "converts keys to kebab case"
      (is (=
            {:some-key 123}
            (wire-json->map "{\"someKey\": 123}"))))

    (testing "preserves keys prefixed with an underscore"
      (is (=
            {:_someLinks 123}
            (wire-json->map "{\"_someLinks\": 123}")))))

  (testing "map->wire-json"
    (testing "returns a json string"
      (is (=
            (long-str
              "{"
              "  \"key\" : 123"
              "}")
            (map->wire-json {:key 123}))))

    (testing "converts dates"
      (is (=
            (long-str
              "{"
              "  \"key\" : \"2019-02-03T00:00:00.000Z\""
              "}")
            (map->wire-json {:key (date/date-time 2019 2 3)}))))

    (testing "converts keys to kebab case"
      (is (=
            (long-str
              "{"
              "  \"someKey\" : 123"
              "}")
            (map->wire-json {:some-key 123}))))

    (testing "preserves meta keys"
      (is (=
            (long-str
              "{"
              "  \"_some-key\" : 123"
              "}")
            (map->wire-json {:_some-key 123})))))

  (testing "db-json->map"
    (testing "parses json"
      (is (=
            {:key 123}
            (db-json->map "{\"key\": 123}"))))

    (testing "converts keys to kebab case"
      (is (=
            {:some-key 123}
            (db-json->map "{\"some_key\": 123}"))))

    (testing "preserves keys prefixed with an underscore"
      (is (=
            {:_some_links 123}
            (db-json->map "{\"_some_links\": 123}")))))

  (testing "map->db-json"
    (testing "returns a json string"
      (is (=
            (long-str
              "{"
              "  \"key\" : 123"
              "}")
            (map->db-json {:key 123}))))

    (testing "converts dates"
      (is (=
            (long-str
              "{"
              "  \"key\" : \"2019-02-03T00:00:00.000Z\""
              "}")
            (map->db-json {:key (date/date-time 2019 2 3)}))))

    (testing "converts keys to snake case"
      (is (=
            (long-str
              "{"
              "  \"some_key\" : 123"
              "}")
            (map->db-json {:some-key 123}))))

    (testing "preserves meta keys"
      (is (=
            (long-str
              "{"
              "  \"_some-key\" : 123"
              "}")
            (map->db-json {:_some-key 123}))))))
