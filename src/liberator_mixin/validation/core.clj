(ns liberator-mixin.validation.core
  (:require
    [halboy.resource :as hal]

    [liberator.util :refer [by-method]]

    [liberator-mixin.util :as util]))

(defprotocol Validator
  (valid? [_ m])
  (problems-for [_ m]))

(defn with-validation []
  {:validate-methods
   [:put :post]

   :processable?
   (fn [context]
     (if-let [new-validator (get-in context [:resource :validator])]
       (let [method (get-in context [:request :request-method])
             validate-methods (get-in context [:resource :validate-methods])]
         (if (some #(= method %) (validate-methods))
           (valid? (new-validator) context)
           true))
       true))

   :handle-unprocessable-entity
   (fn [{:keys [self request resource]}]
     (let [new-validator (:validator resource)
           body (:body request)
           error-id (util/random-uuid)
           error-context (problems-for (new-validator) body)]
       (->
         (hal/new-resource self)
         (hal/add-property :error-id error-id)
         (hal/add-property :error-context error-context))))})

(defn with-validation-mixin [_]
  (with-validation))
