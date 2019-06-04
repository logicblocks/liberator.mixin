(ns liberator-mixin.hypermedia.core
  (:require
    [clojure.string :as str]

    [bidi.bidi :refer [path-for]]
    [liberator-mixin.core :as core]))

(defn base-url [request]
  (let [scheme (-> request :scheme name)
        host (get-in request [:headers "host"])]
    (format "%s://%s" scheme host)))

(defn absolute-url-for
  [request routes handler & args]
  (str
    (base-url request)
    (apply path-for routes handler args)))

(defn parameterised-url-for
  [request routes handler parameter-names & args]
  (let [parameter-names (map name parameter-names)]
    (str
      (apply absolute-url-for request routes handler args)
      (str "{?" (str/join "," parameter-names) "}"))))

(defn with-routes-in-context [routes]
  {:initialize-context (fn [_] {:routes routes})})

(defn with-self-link []
  {:initialize-context
   (fn [{:keys [resource] :as context}]
     (when-let [get-self-link (:self resource)]
       {:self (get-self-link context)}))})

(defn with-hypermedia-mixin [dependencies]
  [(with-routes-in-context (:routes dependencies))
   (with-self-link)])
