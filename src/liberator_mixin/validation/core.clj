(ns liberator-mixin.validation.core
  (:require
    [halboy.resource :as hal]

    [liberator.util :refer [by-method]])
  (:import
    [java.util UUID]))

(defn- random-uuid []
  (str (UUID/randomUUID)))

(defprotocol Validator
  (valid? [_ m])
  (problems-for [_ m]))

(defn with-validation []
  {:validate-methods
   [:put :post]

   :processable?
   (fn [{:keys [resource request] :as context}]
     (if-let [new-validator (get resource :validator)]
       (let [validate-methods (get resource :validate-methods)
             request-method (get request :request-method)]
         (if (some #(= request-method %) (validate-methods))
           (valid? (new-validator context) context)
           true))
       true))

   :handle-unprocessable-entity
   (fn [{:keys [resource] :as context}]
     (let [new-validator (get resource :validator)
           new-error-representation
           (get resource :error-representation
             (fn [{:keys [error-id error-context]}]
               {:error-id error-id :error-context error-context}))

           error-id (random-uuid)
           error-context (problems-for (new-validator context) context)]
       (new-error-representation
         (assoc context
           :error-id error-id
           :error-context error-context))))})

(defn with-validation-mixin [_]
  (with-validation))
