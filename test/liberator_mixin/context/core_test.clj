(ns liberator-mixin.context.core-test
  (:require
    [clojure.test :refer :all]
    [clojure.string :as string]

    [jason.convenience :as jason-conv]

    [ring.mock.request :as ring]

    [liberator-mixin.core :as core]
    [liberator-mixin.context.core :as context]
    [liberator-mixin.json.core :as json]))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body jason-conv/<-wire-json)))

(deftest with-attribute-in-context
  (testing "adds provided attribute to the context"
    (let [routes [["/" :root]]
          resource (core/build-resource
                     (json/with-json-media-type)
                     (context/with-attribute-in-context :routes routes)
                     {:handle-ok
                      (fn [{:keys [routes]}]
                        routes)})
          response (call-resource
                     resource
                     (ring/request :get "/"))]
      (is (= [["/" "root"]] (:body response))))))

(deftest with-attributes-in-context
  (testing "adds provided attributes to the context"
    (let [routes [["/" :root]]
          database (let [state (atom {})]
                     {:put     (fn [[key value]]
                                 (swap! state assoc key value))
                      :get-all (fn [] @state)})
          resource (core/build-resource
                     (json/with-json-media-type)
                     (context/with-attributes-in-context
                       :routes routes
                       :database database)
                     {:handle-ok
                      (fn [{:keys [routes database request]}]
                        (let [{:keys [get-all put]} database
                              query-string (get request :query-string)
                              query-param (string/split query-string #"=")]
                          (put query-param)
                          [routes (get-all)]))})
          response-1 (call-resource
                       resource
                       (ring/request :get "/" {"key1" "value1"}))
          response-2 (call-resource
                       resource
                       (ring/request :get "/" {"key2" "value2"}))]
      (is (= [[["/" "root"]]
              {:key-1 "value1"}]
            (:body response-1)))
      (is (= [[["/" "root"]]
              {:key-1 "value1"
               :key-2 "value2"}]
            (:body response-2))))))
