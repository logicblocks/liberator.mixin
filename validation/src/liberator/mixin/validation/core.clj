(ns liberator.mixin.validation.core
  (:require
   [spec.validate.core :as sv-core]))

;; coercion should be a separate mixin
;; it should happen post validation

(defprotocol Validator
  (valid? [_ m])
  (problems [_ m]))

(defrecord FnBackedValidator
  [valid? problems]
  Validator
  (valid? [_ context] (valid? context))
  (problems [_ context] (problems context)))

(defrecord SpecBackedValidator
  [spec selector problem-subject problem-transformer]
  Validator
  (valid? [_ context]
    (let [s (or selector identity)
          t (s context)
          v (sv-core/validator spec)]
      (v t)))
  (problems [_ context]
    (let [s (or selector identity)
          t (s context)
          pc (sv-core/problem-calculator spec
               :validation-subject
               (if (fn? problem-subject)
                 (problem-subject spec t)
                 (keyword (name spec)))
               :problem-transformer
               (or problem-transformer identity))]
      (pc t))))

(defrecord MultiValidator
  [validators]
  Validator
  (valid? [_ m]
    (every? #(valid? % m) validators))
  (problems [_ m]
    (reduce
      (fn [p v]
        (into p (problems v m)))
      (problems (first validators) m)
      (rest validators))))

(defmulti validator
  (fn [& {:as options}]
    (:type options)))

(defmethod validator :spec
  [& {:as options}]
  (map->SpecBackedValidator (dissoc options :type)))

(defmethod validator :default
  [& {:as options}]
  (map->FnBackedValidator (dissoc options :type)))

(defn spec-validator
  ([spec]
   (spec-validator spec {}))
  ([spec options]
   (map->SpecBackedValidator
     (merge options {:spec spec}))))

(defn combine [first second & rest]
  (->MultiValidator (into [first second] rest)))

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
           error-context (problems (new-validator context) context)]
       (new-error-representation
         (assoc context
           :error-id error-id
           :error-context error-context))))})

(defn with-validation-mixin [_]
  (with-validation))
