(ns liberator-mixin.validation)

(defprotocol Validator
  (valid? [_ m])
  (problems-for [_ m]))
