(ns liberator-mixin.jws-authorisation.core-test
  (:require
    [clojure.test :refer :all]

    [clj-time.core :as time]
    [clj-time.coerce :as tc]

    [ring.mock.request :as ring]
    [buddy.sign.jwt :refer [sign]]
    [liberator-mixin.core :as core]
    [liberator-mixin.jws-authorisation.core :as jws]
    [liberator-mixin.json.core :as json]))

(defn call-resource [resource request]
  (->
    (resource request)))

(deftest with-jws-authorisation
  (testing "the resource is authorised with the right scopes available"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-authorisation ["read"] "foo")
                     {:handle-ok
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

  (testing "the resource is authorised when no scopes required"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (jws/with-jws-authorisation [] "foo")
                     {:handle-ok
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
                     (json/with-body-parsed-as-json)
                     (jws/with-jws-authorisation ["read"] "foo")
                     {:handle-ok
                      (fn [{:keys [routes]}]
                        routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {:scope "write"} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 401 (:status response)))))

  (testing "the resource does not have scope claim"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (json/with-body-parsed-as-json)
                     (jws/with-jws-authorisation ["read"] "foo")
                     {:handle-ok
                      (fn [{:keys [routes]}]
                        routes)})
          request (ring/request :get "/")
          request (ring/header
                    request
                    "authorization"
                    (str "Bearer " (sign {} "foo")))
          response (call-resource
                     resource
                     request)]
      (is (= 401 (:status response)))))

  (testing "the token is under the wrong identifier"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (json/with-body-parsed-as-json)
                     (jws/with-jws-authorisation ["read"] "foo")
                     {:handle-ok
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
      (is (= 401 (:status response)))))

  (testing "the token has expired"
    (let [resource (core/build-resource
                     (json/with-json-media-type)
                     (json/with-body-parsed-as-json)
                     (jws/with-jws-authorisation ["read"] "foo")
                     {:handle-ok
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
                     (json/with-body-parsed-as-json)
                     (jws/with-jws-authorisation
                       ["read"]
                       "foo"
                       :opts {:aud "pms.com"})
                     {:handle-ok
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
                     request)]
      (is (= 401 (:status response))))))