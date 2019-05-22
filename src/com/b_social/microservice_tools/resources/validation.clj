(ns com.b-social.microservice-tools.resources.validation
  (:require [halboy.resource :as hal]
            [liberator.util :refer [by-method]]
            [com.b-social.microservice-tools.data :as data]
            [com.b-social.microservice-tools.validation :as v]))

(defn on-write [f default]
  (by-method
    :put f
    :post f
    :any default))

(defn with-validation []
  {:processable?
   (on-write
     (fn [context]
       (if-let [new-validator (:validator (:resource context))]
         (v/valid? (new-validator) context)
         true))
     true)

   :handle-unprocessable-entity
   (fn [{:keys [self request resource]}]
     (let [new-validator (:validator resource)
           body (:body request)
           error-id (data/random-uuid)
           error-context (v/problems-for (new-validator) body)]
       (->
         (hal/new-resource self)
         (hal/add-property :error-id error-id)
         (hal/add-property :error-context error-context))))})
