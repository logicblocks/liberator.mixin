(ns com.b-social.microservice-tools.resources.core
  (:require
    [com.b-social.microservice-tools.liberator :as liberator]
    [com.b-social.microservice-tools.resources.json :as j]
    [com.b-social.microservice-tools.resources.hypermedia :as h]
    [com.b-social.microservice-tools.resources.validation :as v]))

(defn hal-resource-handler-for [{:keys [routes]} & {:as overrides}]
  (liberator/build-resource
    (h/with-hal-media-type)
    (j/with-body-parsed-as-json)
    (h/with-self-link)
    (h/with-not-found-handler)
    (v/with-validation)
    (h/with-routes-in-context routes)
    overrides))
