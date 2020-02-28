(ns liberator-mixin.authorisation.core-test
  (:require
    [clojure.test :refer :all]

    [clj-time.core :as time]
    [clj-time.coerce :as tc]

    [ring.mock.request :as ring]
    [buddy.sign.jwt :refer [sign]]
    [liberator-mixin.core :as core]
    [liberator-mixin.authorisation.core :as jws]
    [liberator-mixin.json.core :as json]
    [jason.convenience :refer [<-wire-json]]
    [clojure.string :as string]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update-in [:body] <-wire-json)))

(deftest with-jws-authorisation
  (testing "the resource is authorised with the right scopes available"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
                     {:token-claims {:scope (fn [scope]
                                              (every? #(contains? #{"read"} %) (string/split scope #" ")))}
                      :token-key    "foo"
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

  (testing "the resource doesnt care about the case of bearer"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
                     {:token-claims {:scope (fn [scope]
                                              (every? #(contains? #{"read"} %) (string/split scope #" ")))}
                      :token-key    "foo"
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

  (testing "the resource is authorised when no scopes required"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
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
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
                     {:token-claims {:scope (fn [scope]
                                              (every? #(contains? #{"read"} %)
                                                      (string/split scope #" ")))}
                      :token-key    "foo"
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
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
                     {:token-claims {:scope (fn [scope]
                                              (every? #(contains? #{"read"} %)
                                                      (string/split scope #" ")))}
                      :token-key "foo"
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
      (is (string/includes? header "Access token failed validation for scope."))))

  (testing "the token is under the wrong identifier"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
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
            "Message does not contain a Bearer token."))))

  (testing "the token has expired"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
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
                     (jws/with-jws-access-token)
                     (jws/with-www-authenticate)
                     {:token-key "foo"
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
