(ns liberator-mixin.authorisation.core-test
  (:require
    [clojure.test :refer :all]

    [clj-time.core :as time]
    [clj-time.coerce :as tc]

    [ring.mock.request :as ring]
    [buddy.sign.jwt :refer [sign]]
    [liberator-mixin.core :as core]
    [liberator-mixin.authorisation.core :as auth]
    [liberator-mixin.json.core :as json]
    [clojure.string :as string]
    [buddy.core.codecs.base64 :as b64]
    [buddy.core.codecs :as codecs]))

(defn call-resource [resource request]
  (resource request))

(deftype FailedValidator
  []
  auth/ClaimValidator
  (validate [_ _ _] [false]))

(deftest with-jws-authorisation
  (testing "the resource is authorised with a function for the key"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-key        (fn [_] "foo")
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the resource is authorised with the right scopes available"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "openid read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the resource is not authorised with multiple claims"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"}), (FailedValidator.)]
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
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
            "Access token failed validation."))))

  (testing "the resource is not authorised with multiple claims contains first"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"write"}), (FailedValidator.)]
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
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
            "Access token failed validation for scope."))))

  (testing "the resource doesnt care about the case of bearer"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "bearer " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the resource is authorised with a different token scheme"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-type       "Token"
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Token " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the resource is authorised when encoded"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-parser     (fn [token]
                                          (-> (b64/decode token)
                                              (buddy.core.codecs/bytes->str)))
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (codecs/bytes->str
                                     (b64/encode
                                       (sign {:scope "read"} "foo") true))))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the resource is authorised when no scopes required"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok
                                 (fn [{:keys [routes]}]
                                   routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read write"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the resource is not authorised with the right scopes available"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "write"} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 403 (:status response)))
      (is (string/includes?
            header
            "Access token failed validation for scope."))))

  (testing "the resource does not have scope claim"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-validators [(auth/->ScopeValidator #{"read"})]
                      :token-key        "foo"
                      :handle-ok
                                        (fn [{:keys [routes]}]
                                          routes)})
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
                            "Access token failed validation for scope."))))

  (testing "the token is under the wrong identifier"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok
                                 (fn [{:keys [routes]}]
                                   routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Token " (sign {:scope "read"} "foo")))
          response (call-resource
                     resource
                     request)
          header (get-in response [:headers "WWW-Authenticate"])]
      (is (= 400 (:status response)))
      (is (string/includes?
            header
            "Authorisation header does not contain a token."))))

  (testing "the token is not required"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-required? false
                      :handle-ok
                                 (fn [{:keys [routes]}]
                                   routes)})
          request (ring/request :get "/")
          response (call-resource
                     resource
                     request)]
      (is (= 200 (:status response)))))

  (testing "the token is not signed with the same key"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok
                                 (fn [{:keys [routes]}]
                                   routes)})
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
            "Message seems corrupt or manipulated."))))

  (testing "the token has expired"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key "foo"
                      :handle-ok
                                 (fn [{:keys [routes]}]
                                   routes)})
          request (ring/request :get "/")
          expiry-time (tc/to-epoch (time/ago (time/minutes 5)))
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "read"
                                          :exp   expiry-time}
                                         "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 401 (:status response)))))

  (testing "does not meet the required audience"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (auth/with-jws-access-token-mixin)
                     {:token-key     "foo"
                      :token-options {:aud "pms.com"}
                      :handle-ok
                                     (fn [{:keys [routes]}]
                                       routes)})
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
