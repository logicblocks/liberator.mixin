(ns liberator.mixin.logging.core-test
  (:require
   [clojure.test :refer [deftest is]]

   [cartus.test :as ct]

   [ring.mock.request :as ring]

   [jason.convenience :as jason-conv]

   [liberator.mixin.core :as core]
   [liberator.mixin.logging.core :as logging]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(deftest with-logger-adds-logger-from-dependencies-to-resource
  (let [logger (ct/logger)
        resource (core/build-resource
                   {:available-media-types ["application/json"]}
                   (logging/with-logger {:logger logger})
                   {:handle-ok
                    (fn [{:keys [resource]}]
                      (if (= ((get resource :logger)) logger)
                        {:result true}
                        {:result false}))})
        response (call-resource
                   resource
                   (ring/request :get "/"))]
    (is (= {:result true} (:body response)))))
