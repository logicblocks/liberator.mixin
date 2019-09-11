(ns liberator-mixin.context.core)

(defn with-attribute-in-context [key value]
  {:initialize-context (fn [_] {key value})})
