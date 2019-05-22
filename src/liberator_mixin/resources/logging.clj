(ns liberator-mixin.resources.logging)

(defprotocol Logger
  (log-error [self message context cause]))
