(ns liberator.mixin.hypermedia.core-test
  (:require
   [clojure.test :refer :all]

   [ring.mock.request :as ring]

   [jason.convenience :as jason-conv]

   [liberator.mixin.core :as core]
   [liberator.mixin.json.core :as json]
   [liberator.mixin.hypermedia.core :as hypermedia]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(deftest with-self-link
  (testing "adds a self link to context"
    (let [self-link "https://self.example.com"
          resource (core/build-resource
                     (json/with-json-media-type)
                     (hypermedia/with-self-link)
                     {:self-link (constantly self-link)
                      :handle-ok
                      (fn [{:keys [self-link]}]
                        {:self self-link})})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/")
                       :accept json/json-media-type))]
      (is (=
            self-link
            (get-in response [:body :self]))))))

(deftest with-router-in-context
  (testing "adds router from dependencies to context"
    (let [router ["" [["/" :root]]]
          resource (core/build-resource
                     (json/with-json-media-type)
                     (hypermedia/with-router-in-context
                       {:router router})
                     {:handle-ok
                      (fn [{:keys [router]}]
                        {:router router})})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/")
                       :accept json/json-media-type))]
      (is (= ["" [["/" "root"]]]
            (get-in response [:body :router]))))))
