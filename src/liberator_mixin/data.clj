(ns liberator-mixin.data
  (:import [java.util UUID]))

(defn random-uuid []
  (str (UUID/randomUUID)))
