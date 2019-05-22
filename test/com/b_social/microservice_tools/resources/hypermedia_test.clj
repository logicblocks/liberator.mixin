(ns com.b-social.microservice-tools.resources.hypermedia-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as ring]
            [com.b-social.microservice-tools.json :as json]
            [com.b-social.microservice-tools.liberator :as l]
            [com.b-social.microservice-tools.resources.logging :as log]
            [com.b-social.microservice-tools.resources.hypermedia :as r]))

(deftype TestLogger [state]
  log/Logger
  (log-error [_ message context cause]
    (reset!
      state
      {:message message
       :context context
       :cause   cause})))

(defn new-test-logger [state]
  (->TestLogger state))

(defn call-resource [resource request]
  (->
    (resource request)
    (update :body json/wire-json->map)))

(deftest hypermedia-mixins
  (testing "with-routes-in-context"
    (testing "adds routes to the context"
      (let [routes [["/" :root]]
            resource (l/build-resource
                       (r/with-routes-in-context routes)
                       {:handle-ok
                        (fn [{:keys [routes]}]
                          routes)})
            response (call-resource
                       resource
                       (ring/request :get "/"))]
        (is (some? (:body response))))))

  (testing "with-hal-media-type"
    (testing "allows hypermedia requests"
      (let [resource (l/build-resource
                       (r/with-hal-media-type)
                       {:handle-ok (constantly {:status "OK"})})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept r/hal-media-type))]
        (is (= 200 (:status response)))
        (is (= {:status "OK"}
              (:body response)))))

    (testing "correctly responds when unauthorised"
      (let [resource (l/build-resource
                       (r/with-hal-media-type)
                       {:authorized? false
                        :handle-unauthorized {:error "unauthorised"}})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept r/hal-media-type))]
        (is (= 401 (:status response)))
        (is (= {:error "unauthorised"}
              (:body response))))))

  (testing "with-self-link"
    (testing "adds a self link to context"
      (let [self-link "https://self.example.com"
            resource (l/build-resource
                       (r/with-hal-media-type)
                       (r/with-self-link)
                       {:self (constantly self-link)
                        :handle-ok
                              (fn [{:keys [self]}]
                                {:self self})})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept r/hal-media-type))]
        (is (=
              self-link
              (get-in response [:body :self]))))))

  (testing "with-not-found-handler"
    (testing "provides a sensible default when the resource does not exist"
      (let [resource (l/build-resource
                       (r/with-hal-media-type)
                       (r/with-not-found-handler)
                       {:exists? (constantly false)})
            response (call-resource
                       resource
                       (ring/header
                         (ring/request :get "/")
                         :accept r/hal-media-type))]
        (is (= 404 (:status response)))
        (is (= "Resource not found"
              (get-in response [:body :error]))))))

  (testing "with-exception-handler"
    (let [log-state-atom (atom {})
          resource (l/build-resource
                     (r/with-hal-media-type)
                     (r/with-exception-handler)
                     {:logger
                      (constantly (new-test-logger log-state-atom))
                      :handle-ok
                      #(throw (ex-info "Something went wrong" {}))})
          response (call-resource
                     resource
                     (ring/header
                       (ring/request :get "/")
                       :accept r/hal-media-type))]
      (testing "returns a json response when an exception is thrown"
        (is (= 500 (:status response)))
        (is (some? (get-in response [:body :error-id])))
        (is (= "Request caused an exception"
              (get-in response [:body :message]))))

      (testing "calls the logger when present"
        (let [log-state @log-state-atom]
          (is (= "Request caused an exception"
                (:message log-state)))
          (is (some? (:context log-state)))
          (is (some? (:cause log-state))))))))
