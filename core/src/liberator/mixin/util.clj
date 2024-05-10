(ns liberator.mixin.util)

(defn resource-attribute-as-value [{:keys [resource] :as context} attribute]
  (let [attribute-fn (attribute resource)]
    (when attribute-fn
      (attribute-fn context))))

(defn resource-attribute-as-fn [{:keys [resource] :as context} attribute]
  (let [attribute-fn (attribute resource)]
    (if attribute-fn
      (partial attribute-fn context)
      (constantly nil))))
