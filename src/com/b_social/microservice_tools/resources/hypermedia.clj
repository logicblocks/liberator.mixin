(ns com.b-social.microservice-tools.resources.hypermedia
  (:require
    [halboy.resource :as hal]
    [halboy.json :as hal-json]
    [liberator.representation :as r]
    [com.b-social.microservice-tools.urls :as urls]
    [com.b-social.microservice-tools.json :as json]
    [com.b-social.microservice-tools.liberator :as liberator]
    [com.b-social.microservice-tools.resources.json :as json-resources]))

(def hal-media-type "application/hal+json")

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

(defn with-hal-media-type []
  {:available-media-types [hal-media-type]})

(defn with-routes-in-context [routes]
  {:initialize-context (fn [_] {:routes routes})})

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
    (json-resources/with-body-parsed-as-json)
    (with-not-found-handler)
    (with-routes-in-context routes)
    overrides))

