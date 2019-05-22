(ns liberator-mixin.resources.core
  (:require
    [liberator-mixin.liberator :as liberator]
    [liberator-mixin.resources.json :as j]
    [liberator-mixin.resources.hypermedia :as h]
    [liberator-mixin.resources.validation :as v]))

(defn hal-resource-handler-for [{:keys [routes]} & {:as overrides}]
  (liberator/build-resource
    (h/with-hal-media-type)
    (j/with-body-parsed-as-json)
    (h/with-self-link)
    (h/with-not-found-handler)
    (v/with-validation)
    (h/with-routes-in-context routes)
    overrides))
