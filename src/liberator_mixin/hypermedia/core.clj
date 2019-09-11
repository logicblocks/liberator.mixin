(ns liberator-mixin.hypermedia.core
  (:require
    [liberator-mixin.context.core :refer [with-attribute-in-context]]))

(defn with-self-link []
  {:initialize-context
   (fn [{:keys [resource] :as context}]
     (when-let [get-self-link (:self resource)]
       {:self (get-self-link context)}))})

(defn with-hypermedia-mixin [dependencies]
  [(with-attribute-in-context :routes (:routes dependencies))
   (with-self-link)])
