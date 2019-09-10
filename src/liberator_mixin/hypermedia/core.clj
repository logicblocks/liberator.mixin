(ns liberator-mixin.hypermedia.core)

(defn with-routes-in-context [routes]
  {:initialize-context (fn [_] {:routes routes})})

(defn with-self-link []
  {:initialize-context
   (fn [{:keys [resource] :as context}]
     (when-let [get-self-link (:self resource)]
       {:self (get-self-link context)}))})

(defn with-hypermedia-mixin [dependencies]
  [(with-routes-in-context (:routes dependencies))
   (with-self-link)])
