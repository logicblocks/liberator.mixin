(ns liberator.mixin.logging.core)

(defprotocol Logger
  (log-error [self message context cause]))

(defn with-logger
  "This sets a logger on the resource for use with other mixins such as
  hal/with-exception-handler."
  [dependencies]
  {:logger (:logger dependencies)})
