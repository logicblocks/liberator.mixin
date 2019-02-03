(ns com.b-social.microservice-tools.resources
  (:require
    [halboy.resource :as hal]
    [halboy.json :as hal-json]
    [liberator.representation :as r]
    [com.b-social.microservice-tools.urls :as urls]
    [com.b-social.microservice-tools.json :as json]
    [com.b-social.microservice-tools.liberator :as liberator])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(def json-media-type "application/json")
(def hal-media-type "application/hal+json")

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (seq (re-find #"^application/(.+\+)?json" type))))

(defn- read-json [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (json/wire-json->map body-string)]
          (catch JsonParseException ex
            [false nil]))))))

(extend-protocol r/Representation
  halboy.resource.Resource
  (as-response [data {:keys [request routes] :as context}]
    (r/as-response
      (-> data
        (hal/add-link
          :discovery
          {:href (urls/absolute-url-for request routes :discovery)})
        (hal-json/resource->map))
      context)))

(defmethod r/render-map-generic hal-media-type [data _]
  (json/map->wire-json data))

(defn with-json-media-type []
  {:available-media-types [json-media-type]})

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

