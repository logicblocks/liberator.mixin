(ns microservice-tools.core
  (:require
    [liberator.representation
     :refer [render-map-generic
             as-response
             Representation]]

    [halboy.resource :as hal]

    [microservice-tools.hypermedia
     :refer [absolute-url-for]]
    [microservice-tools.liberator :as liberator]
    [microservice-tools.json
     :refer [wire-json->map
             map->wire-json]]
    [clojure.string
     :refer [upper-case starts-with?]])
  (:import [com.fasterxml.jackson.core JsonParseException]))


(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (seq (re-find #"^application/(.+\+)?json" type))))

(defn- read-json [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (wire-json->map body-string)]
          (catch JsonParseException ex
            [false nil]))))))


(def hal-media-type "application/hal+json")

(defmethod render-map-generic hal-media-type [data _]
  (map->wire-json data))

(defn with-hal-media-type []
  {:available-media-types [hal-media-type]})

(defn with-routes-in-context [routes]
  {:initialize-context (fn [_] {:routes routes})})

(defn with-body-parsed-as-json []
  {:initialize-context
   (fn [{:keys [request]}]
     (if-let [[valid? json] (read-json request)]
       (if valid?
         {:request {:body json}}
         {:malformed? true})))

   :malformed?
   (fn [{:keys [malformed?]}] malformed?)})

(defn with-not-found-handler []
  {:handle-not-found
   (fn [{:keys [not-found-message]
         :or   {not-found-message "Resource not found"}}]
     (hal/add-properties
       (hal/new-resource)
       {:error not-found-message}))})

(defn hal-resource-handler-for [{:keys [routes]} & {:as overrides}]
  (liberator/build-resource
    (with-hal-media-type)
    (with-body-parsed-as-json)
    (with-not-found-handler)
    (with-routes-in-context routes)
    overrides))

