(ns com.b-social.microservice-tools.resources.logging)

(defprotocol Logger
  (log-error [self message context cause]))
