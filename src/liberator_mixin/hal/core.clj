(ns liberator-mixin.hal.core
  (:require
    [halboy.resource :as hal]
    [halboy.json :as hal-json]

    [jason.core :as jason]

    [liberator.representation :as r]

    [liberator-mixin.hypermedia.core :as hypermedia]
    [liberator-mixin.logging.core :as log])
  (:import
    [halboy.resource Resource]
    [java.util UUID]))

(defn- random-uuid []
  (str (UUID/randomUUID)))

(def hal-media-type "application/hal+json")

(def ^:private ->wire-json (jason/new-json-encoder))

(extend-protocol r/Representation
  Resource
  (as-response [data {:keys [request routes] :as context}]
    (r/as-response
      (-> data
        (hal/add-link :discovery
          (hypermedia/absolute-url-for request routes :discovery))
        (hal-json/resource->map))
      context)))

(defmethod r/render-map-generic hal-media-type [data _]
  (->wire-json data))

(defn with-hal-media-type []
  {:available-media-types
   [hal-media-type]

   :service-available?
   {:representation {:media-type hal-media-type}}})

(defn with-exception-handler []
  {:handle-exception
   (fn [{:keys [exception resource]}]
     (let [error-id (random-uuid)
           message "Request caused an exception"]
       (when-let [get-logger (:logger resource)]
         (log/log-error (get-logger) message
           {:error-id error-id}
           exception))
       (hal/add-properties
         (hal/new-resource)
         {:error-id error-id
          :message message})))})

(defn with-not-found-handler []
  {:handle-not-found
   (fn [{:keys [not-found-message]
         :or   {not-found-message "Resource not found"}}]
     (hal/add-properties
       (hal/new-resource)
       {:error not-found-message}))})

(defn with-hal-mixin [_]
  [(with-hal-media-type)
   (with-not-found-handler)])
