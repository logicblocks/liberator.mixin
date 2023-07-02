(ns liberator.mixin.authorisation.core-test
  (:require
   [clojure.test :refer :all]
   [tick.core :as t]
   [ring.mock.request :as ring]
   [buddy.sign.jwt :refer [sign]]
   [liberator.mixin.core :as core]
   [liberator.mixin.authorisation.core :as auth]
   [liberator.mixin.json.core :as json]
   [clojure.string :as string]
   [buddy.core.codecs :as codecs]))

(defn call-resource [resource request]
  (resource request))

(deftype FailedValidator
  []
  auth/ClaimValidator
  (validate [_ _ _] [false]))

(deftest with-function-for-key
  (testing "the resource is authorised with a function for the key"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator {:get #{"read"}})]
             :token-key        (fn [_] "foo")
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest with-function-for-auth-header
  (testing (str "the resource is authorised with a function for the name of "
             "the authorisation header")
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators  [(auth/->ScopeValidator {:get #{"read"}})]
             :token-key         (fn [_] "foo")
             :token-header-name (fn [& _] "x-auth-jwt")
             :handle-ok         (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "x-auth-jwt"
                    (str "Bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest with-right-scopes
  (testing "the resource is authorised with the right scopes available"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator
                                  {:get  #{"read"}
                                   :post #{"write"}})]
             :token-key        "foo"
             :allowed-methods  [:get :post]
             :handle-ok        (fn [_] {:status :success})
             :handle-created   (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "openid read"} "foo")))
          response (call-resource
                     resource
                     request)

          post-request (ring/request :post "/")
          post-request (ring/header
                         post-request
                         "authorization"
                         (str "Bearer " (sign {:scope "openid write"} "foo")))
          post-response (call-resource
                          resource
                          post-request)]
      (is (= 200 (:status response)))
      (is (= 201 (:status post-response))))))

(deftest not-authorised-with-multiple-claims
  (testing "the resource is not authorised with multiple claims"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator {:get #{"read"}}),
                                (FailedValidator.)]
             :token-key        "foo"
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 403 (:status response)))
      (is (string/includes?
            header
            "Access token failed validation.")))))

(deftest not-authorised-with-multiple-claims-first
  (testing "the resource is not authorised with multiple claims contains first"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator
                                  {:get #{"write"}}),
                                (FailedValidator.)]
             :token-key        "foo"
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 403 (:status response)))
      (is (string/includes?
            header
            "Access token failed validation for scope.")))))

(deftest bearer-any-case
  (testing "the resource doesnt care about the case of bearer"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator {:get #{"read"}})]
             :token-key        "foo"
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest with-different-token-scheme
  (testing "the resource is authorised with a different token scheme"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator {:get #{"read"}})]
             :token-type       "Token"
             :token-key        "foo"
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Token " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest with-encoding
  (testing "the resource is authorised when encoded"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator {:get #{"read"}})]
             :token-parser     (fn [token]
                                 (-> token
                                   (codecs/str->bytes)
                                   (codecs/b64->bytes)
                                   (codecs/bytes->str)))
             :token-key        "foo"
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer "
                      (-> (sign {:scope "read"} "foo")
                        (codecs/str->bytes)
                        (codecs/bytes->b64)
                        (codecs/bytes->str))))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest when-no-scopes-required
  (testing "the resource is authorised when no scopes required"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read write"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest with-right-scopes-available
  (testing "the resource is not authorised with the right scopes available"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator
                                  {:get  #{"not-read"}
                                   :post #{"not-write"}})]
             :token-key        "foo"
             :allowed-methods  [:get :post]
             :handle-ok        (fn [_] {:status :success})
             :handle-created   (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])

          post-request (ring/request :post "/")
          post-request (ring/header
                         post-request
                         "authorization"
                         (str "Bearer " (sign {:scope "openid write"} "foo")))
          post-response (call-resource
                          resource
                          post-request)

          post-header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 403 (:status response)))
      (is (string/includes?
            header
            "Access token failed validation for scope."))
      (is (= 403 (:status post-response)))
      (is (string/includes?
            post-header
            "Access token failed validation for scope.")))))

(deftest with-wrong-scope-claim
  (testing "the resource does not have scope claim"
    (let [resource
          (core/build-resource
            (json/with-json-media-type)
            (auth/with-jws-access-token-mixin)
            {:token-validators [(auth/->ScopeValidator {:get #{"read"}})]
             :token-key        "foo"
             :handle-ok        (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 403 (:status response)))
      (is (string/includes? header
            "Access token failed validation for scope.")))))

(deftest with-token-under-wrong-identifier
  (testing "the token is under the wrong identifier"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Token " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 401 (:status response)))
      (is (string/includes?
            header
            "Authorisation header does not contain a token.")))))

(deftest with-token-not-required
  (testing "the token is not required"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-required? {:any false}
                      :handle-ok       (fn [_] {:status :success})})
          request (ring/request :get "/")
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response))))))

(deftest with-token-not-required-on-verb
  (testing "the token is not required on verb"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-required? {:post true
                                        :get  false}
                      :allowed-methods [:post :get]
                      :handle-ok       (fn [_] {:status :success})
                      :handle-created  (fn [_] {:status :success})})]
      (is (= 200 (:status (call-resource
                            resource
                            (ring/request :get "/")))))
      (is (= 401 (:status (call-resource
                            resource
                            (ring/request :post "/"))))))))

(deftest with-token-not-signed-with-same-key
  (testing "the token is not signed with the same key"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"} "bar")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 401 (:status response)))
      (is (string/includes?
            header
            "Message seems corrupt or manipulated.")))))

(deftest with-expired-token
  (testing "the token has expired"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok (fn [_] {:status :success})})
          request (ring/request :get "/")
          expiry-time (t/long (t/<< (t/now) (t/new-duration 5 :minutes)))
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"
                                          :exp   expiry-time}
                                     "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 401 (:status response))))))

(deftest with-invalid-audience
  (testing "does not meet the required audience"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key     "foo"
                      :token-options {:aud "pms.com"}
                      :handle-ok     (fn [_] {:status :success})})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"
                                          :aud   "developers.com"}
                                     "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 401 (:status response)))
      (is (string/includes? header "Audience does not match pms.com")))))
