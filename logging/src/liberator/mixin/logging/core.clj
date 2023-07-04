(ns liberator.mixin.logging.core
  (:require
   [cartus.null :as cn]))

(defn with-logger
  "Returns a mixin which adds the logger at `:logger` in the provided
  dependencies map to the resource under key `:logger`.

  The logger must be an implementation of [[cartus.core/Logger]]. If the
  dependency map does not contain a value for key `:logger`, a
  [[cartus.null/NullLogger]] will be added instead.

  The logger is used by other mixins (such as
  [[liberator.mixin.hal.core/with-exception-handler]]) when they need to
  perform any logging."
  [dependencies]
  {:logger (get dependencies :logger (cn/logger))})
