(ns liberator.mixin.hypermedia.core
  (:require
   [liberator.mixin.context.core :refer [with-attributes-in-context]]))

(defn with-self-link []
  {:initialize-context
   (fn [{:keys [resource] :as context}]
     (when-let [self-link (:self-link resource)]
       {:self-link (self-link context)}))})

(defn with-router-in-context [dependencies]
  (with-attributes-in-context
    (select-keys dependencies [:router])))

(defn with-hypermedia-mixin
  ([]
   (with-hypermedia-mixin {}))
  ([dependencies]
   [(with-router-in-context dependencies)
    (with-self-link)]))
