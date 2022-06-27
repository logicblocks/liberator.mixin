(ns liberator-mixin.authorisation.unverified-test
  (:require
    [clojure.test :refer :all]

    [ring.mock.request :as ring]
    [buddy.sign.jwt :refer [sign]]
    [liberator-mixin.core :as core]
    [liberator-mixin.authorisation.core :as auth]
    [liberator-mixin.authorisation.unverified :refer [with-jwt-scopes]]
    [liberator-mixin.json.core :as json]
    [clojure.string :as string]))


(deftest with-jwt-scopes-give-no-jwt
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {})
        request (ring/request :get "/")

        response (handler request)
        header (get-in response [:headers "WWW-Authenticate"])]
    (testing "has Unauthorized status"
      (is (= 401 (:status response))))
    (testing "has appropriate error message"
      (is (string/includes? header "error=\"invalid_token\""))
      (is (string/includes? header "error_message=\"No x-auth-jwt token\"")))))

(deftest with-jwt-scopes-given-unparsable-jwt
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {})
        request (ring/header (ring/request :get "/")
                  "x-auth-jwt"
                  "Bearer 123456")

        response (handler request)
        header (get-in response [:headers "WWW-Authenticate"])]
    (testing "has Unauthorized status"
      (is (= 401 (:status response))))
    (testing "has appropriate error message"
      (is (string/includes? header
            "error=\"invalid_token\""))
      (is (string/includes? header
            "error_message=\"The token was expected to have 3 parts, but got 1.\"")))))

(deftest with-jwt-scopes-given-jwt-with-scopes
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:allowed?
                   (fn [{:keys [scopes]}]
                     (= scopes #{"read" "write"}))})

        request (ring/header
                  (ring/request :get "/")
                  "x-auth-jwt"
                  (str "Bearer " (sign {:scope "read write"} "bar")))

        response (handler request)]
    (testing "provided correct scopes to allowed function"
      (is (= 200 (:status response))))))