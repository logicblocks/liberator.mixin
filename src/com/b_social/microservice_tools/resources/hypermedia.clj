(ns com.b-social.microservice-tools.resources.hypermedia
  (:require
    [halboy.resource :as hal]
    [halboy.json :as hal-json]
    [liberator.representation :as r]
    [com.b-social.microservice-tools.urls :as urls]
    [com.b-social.microservice-tools.json :as json]
    [com.b-social.microservice-tools.resources.logging :as log]
    [com.b-social.microservice-tools.data :as data]))

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
  {:available-media-types
   [hal-media-type]

   :service-available?
   {:representation {:media-type hal-media-type}}})

(defn with-routes-in-context [routes]
  {:initialize-context (fn [_] {:routes routes})})

(defn with-self-link []
  {:initialize-context
   (fn [{:keys [resource] :as context}]
     (when-let [get-self-link (:self resource)]
       {:self (get-self-link context)}))})

(defn with-exception-handler []
  {:handle-exception
   (fn [{:keys [exception resource]}]
     (let [error-id (data/random-uuid)
           message "Request caused an exception"]
       (do
         (when-let [get-logger (:logger resource)]
           (log/log-error
             (get-logger)
             message
             {:error-id error-id}
             exception))
         (hal/add-properties
           (hal/new-resource)
           {:error-id error-id
            :message  message}))))})

(defn with-not-found-handler []
  {:handle-not-found
   (fn [{:keys [not-found-message]
         :or   {not-found-message "Resource not found"}}]
     (hal/add-properties
       (hal/new-resource)
       {:error not-found-message}))})
