(ns liberator-mixin.context.core
  "Liberator mixins to add attributes to the context map for all requests.")

(defn with-attribute-in-context
  "Returns a mixin that adds an attribute with specified `value` to the context
  map at `key` for all requests.

  This mixin can be used multiple times for different attributes."
  [key value]
  {:initialize-context (fn [_] {key value})})

(defn with-attributes-in-context
  "Returns a mixin that adds all attributes supplied as key / value pairs
  to the context map for all requests.

  This mixin can be used multiple times."
  [& {:as attributes}]
  {:initialize-context (fn [_] attributes)})
