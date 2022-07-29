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


(deftest with-jwt-scopes-given-no-jwt
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (auth/with-bearer-token)
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
                  (auth/with-bearer-token)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:token-header-name "x-auth-jwt"
                   :token-type "Bearer"})
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
                  (auth/with-bearer-token)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:token-header-name "x-auth-jwt"
                   :token-type nil
                   :allowed?
                   (fn [{:keys [scopes]}]
                     (= scopes #{"read" "write"}))})

        request (ring/header
                  (ring/request :get "/")
                  "x-auth-jwt"
                   (sign {:scope "read write"} "bar"))

        response (handler request)]
    (testing "provided correct scopes to allowed function"
      (is (= 200 (:status response))))))

(deftest with-jwt-scopes-given-bearer-jwt-with-scopes
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (auth/with-bearer-token)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:token-header-name "x-auth-jwt"
                   :allowed?
                   (fn [{:keys [scopes]}]
                     (= scopes #{"read" "write"}))})

        request (ring/header
                  (ring/request :get "/")
                  "x-auth-jwt"
                  (str "Bearer " (sign {:scope "read write"} "bar")))

        response (handler request)]
    (testing "provided correct scopes to allowed function"
      (is (= 200 (:status response))))))

(deftest with-jwt-scopes-given-optional-bearer-jwt-with-scopes
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (auth/with-bearer-token)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:token-header-name "x-auth-jwt"
                   :token-type        ["Bearer" nil]
                   :allowed?
                   (fn [{:keys [scopes]}]
                     (= scopes #{"read" "write"}))})

        request-1 (ring/header
                  (ring/request :get "/")
                  "x-auth-jwt"
                  (str "bearer " (sign {:scope "read write"} "bar")))
        response-1 (handler request-1)

        request-2 (ring/header
                    (ring/request :get "/")
                    "x-auth-jwt"
                    (str "bearer " (sign {:scope "read write"} "bar")))
        response-2 (handler request-2)]
    (testing "provided correct scopes to allowed function"
      (is (= 200 (:status response-1)))
      (is (= 200 (:status response-2))))))

(deftest with-jwt-scopes-token-not-required-given-no-jwt
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (auth/with-bearer-token)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:token-header-name "x-auth-jwt"
                   :token-required? false})

        request (ring/request :get "/")

        response (handler request)]
    (testing "provided correct scopes to allowed function"
      (is (= 200 (:status response))))))

(deftest with-jwt-scopes-token-not-required-for-method-given-no-jwt
  (let [handler (core/build-resource
                  (json/with-json-media-type)
                  (auth/with-bearer-token)
                  (with-jwt-scopes)
                  (auth/with-www-authenticate-header)
                  {:allowed-methods [:get :post]
                   :token-header-name "x-auth-jwt"
                   :token-required?   {:get  false
                                       :post true}})

        get-response (handler (ring/request :get "/"))
        post-response (handler (ring/request :post "/"))
        post-header (get-in post-response [:headers "WWW-Authenticate"])
        ]
    (testing "provided correct scopes to allowed function"
      (is (= 200 (:status get-response))))

    (testing "has Unauthorized status"
      (is (= 401 (:status post-response))))
    (testing "has appropriate error message"
      (is (string/includes? post-header "error=\"invalid_token\""))
      (is (string/includes? post-header "error_message=\"No x-auth-jwt token\"")))))