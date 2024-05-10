(ns liberator.mixin.util-test
  (:require
   [clojure.test :refer :all]

   [ring.mock.request :as ring]

   [liberator.mixin.core :as core]
   [liberator.mixin.util :as util]))

(deftest resource-attribute-as-value-for-static-value
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :thing           "value"
           :handle-ok
           (fn [context]
             (util/resource-attribute-as-value context :thing))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= "value" (:body response)))))

(deftest resource-attribute-as-value-for-function-value
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :thing           (fn [{:keys [request]}] (:server-name request))
           :handle-ok
           (fn [context]
             (util/resource-attribute-as-value context :thing))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= "example.com" (:body response)))))

(deftest resource-attribute-as-value-for-missing-attribute
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :handle-ok
           (fn [context]
             (util/resource-attribute-as-value context :thing))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= nil (:body response)))))

(deftest resource-attribute-as-fn-for-static-value
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :thing           "value"
           :handle-ok
           (fn [context]
             (let [attribute-fn
                   (util/resource-attribute-as-fn context :thing)]
               (attribute-fn)))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= "value" (:body response)))))

(deftest resource-attribute-as-fn-for-function-value-of-context-only
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :thing           (fn [{:keys [request]}] (:server-name request))
           :handle-ok
           (fn [context]
             (let [attribute-fn
                   (util/resource-attribute-as-fn context :thing)]
               (attribute-fn)))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= "example.com" (:body response)))))

(deftest resource-attribute-as-fn-for-function-value-of-context-and-other-args
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :thing           (fn [{:keys [request]} other]
                              (str (:server-name request) "-" other))
           :handle-ok
           (fn [context]
             (let [attribute-fn
                   (util/resource-attribute-as-fn context :thing)]
               (attribute-fn "otherarg")))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= "example.com-otherarg" (:body response)))))

(deftest resource-attribute-as-fn-for-missing-attribute
  (let [resource
        (core/build-resource
          {:allowed-methods [:get]
           :handle-ok
           (fn [context]
             (let [attribute-fn
                   (util/resource-attribute-as-fn context :thing)]
               (attribute-fn)))})
        request
        (ring/request :get "http://example.com/events")
        response (resource request)]
    (is (= nil (:body response)))))
