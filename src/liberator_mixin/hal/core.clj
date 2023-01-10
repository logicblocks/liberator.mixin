(ns liberator-mixin.hal.core
  "A liberator mixin to add
  [HAL](https://tools.ietf.org/html/draft-kelly-json-hal-00) support to
  liberator.

  In short:

    - Adds `application/hal+json` as a supported media type.
    - Adds support for JSON serialisation for maps and seqs for the HAL
      media type.
    - Adds support for [halboy](https://github.com/jimmythompson/halboy)
      resource serialisation.
    - Adds a default handler responding with an empty HAL resource for
      `:handle-not-found`.
    - Adds a HAL error representation for use with the
      [[liberator-mixin.validation.core|validation mixin]].

  Depends on:

    - the [[liberator-mixin.json.core|JSON mixin]],
    - the [[liberator-mixin.hypermedia.core|hypermedia mixin]].

  Optionally extends:

    - the [[liberator-mixin.validation.core|validation mixin]].

  ### JSON serialisation support

  The JSON serialisation support for the `application/hal+json` media type uses
  the same underlying JSON encoder as the
  [[liberator-mixin.json.core|JSON mixin]]. If that mixin is configured with a
  custom JSON encoder, all `application/hal+json` responses will use the custom
  JSON encoder.

  ### halboy `Resource` support

  The [halboy](https://github.com/jimmythompson/halboy) resource support
  will add a `:discovery` link to any returned resource and expects `bidi`
  `:routes` to be available in the `context`, containing a route named
  `:discovery`. The [[liberator-mixin.hypermedia.core|hypermedia mixin]] adds
  routes to the `context` so nothing further is needed if that mixin is in use."
  (:require
    [halboy.resource :as hal]
    [liberator.representation :as r]

    [halboy.json :as haljson]

    [hype.core :as hype]

    [liberator-mixin.logging.core :as log]
    [jason.convenience :as jason-conv])
  (:import
    [halboy.resource Resource]
    [java.util UUID]))

(defn- random-uuid []
  (str (UUID/randomUUID)))

(def hal-media-type
  "The HAL media type string."
  "application/hal+json")

(extend-protocol r/Representation
  Resource
  (as-response [data {:keys [request routes] :as context}]
    (r/as-response
      (-> data
        (hal/add-link :discovery
          (hype/absolute-url-for request routes :discovery))
        (haljson/resource->map))
      context)))

(defmethod r/render-map-generic hal-media-type [data {:keys [json]}]
  ((get json :encoder jason-conv/->wire-json) data))

(defmethod r/render-seq-generic hal-media-type [data {:keys [json]}]
  ((get json :encoder jason-conv/->wire-json) data))

(defn with-hal-media-type
  "Returns a mixin to add the HAL media type to the available media types."
  []
  {:available-media-types
   [hal-media-type]

   :service-available?
   {:representation {:media-type hal-media-type}}})

(defn with-hal-error-representation
  "Returns a mixin adding a HAL error representation factory function to the
  resource, at `:error-representation`, for use by other mixins, such as
  `liberator-mixin.validation` when they need to render errors.

  The error representation factory function expects the context to include
  a `:self` href, an `:error-id` and an `:error-context` used in the resulting
  representation."
  []
  {:error-representation
   (fn [{:keys [self error-id error-context]}]
     (->
       (hal/new-resource self)
       (hal/add-property :error-id error-id)
       (hal/add-property :error-context error-context)))})

(defn with-exception-handler
  "Returns a mixin which adds a generic exception handler, logging the
  exception and returning an error representation masking the exception.

  This mixin expects a `:logger` to be present on the resource. If no `:logger`
  is found, nothing will be logged."
  []
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

(defn with-unauthorized-handler []
  {:handle-unauthorized
   (fn [{:keys [unauthorized-message]
         :or   {unauthorized-message "Unauthorized"}}]
     (hal/add-properties
       (hal/new-resource)
       {:error unauthorized-message}))})

(defn with-forbidden-handler []
  {:handle-forbidden
   (fn [{:keys [forbidden-message]
         :or   {forbidden-message "Forbidden"}}]
     (hal/add-properties
       (hal/new-resource)
       {:error forbidden-message}))})

(defn with-method-not-allowed-handler []
  {:handle-method-not-allowed
   (fn [{:keys [method-not-allowed-message]
         :or   {method-not-allowed-message "Method not allowed"}}]
     (hal/add-properties
       (hal/new-resource)
       {:error method-not-allowed-message}))})

(defn with-hal-mixin [_]
  [(with-hal-media-type)
   (with-hal-error-representation)
   (with-exception-handler)
   (with-not-found-handler)
   (with-unauthorized-handler)
   (with-forbidden-handler)
   (with-method-not-allowed-handler)])
