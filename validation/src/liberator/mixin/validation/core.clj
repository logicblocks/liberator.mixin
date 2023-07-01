(ns liberator.mixin.validation.core
  (:refer-clojure :exclude [random-uuid])
  (:import
   [java.util UUID]))

(defn- random-uuid []
  (str (UUID/randomUUID)))

(defprotocol Validator
  (valid? [_ m])
  (problems-for [_ m]))

(defrecord FnBackedValidator [valid-fn problems-for-fn]
           Validator
           (valid? [_ context] (valid-fn context))
           (problems-for [_ context] (problems-for-fn context)))

(defn validator [& {:as options}]
  (map->FnBackedValidator options))

(defn with-validation []
  {:validate-methods
   (fn [{:keys [resource]}] ((:known-methods resource)))

   :processable?
   (fn [{:keys [resource request] :as context}]
     (if-let [new-validator (get resource :validator)]
       (if-let [validator (new-validator context)]
         (let [validate-methods (get resource :validate-methods)
               request-method (get request :request-method)]
           (if (some #(= request-method %) (validate-methods context))
             (valid? validator context)
             true))
         true)
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
