(ns liberator-mixin.logging.core)

(defprotocol Logger
  (log-error [self message context cause]))
