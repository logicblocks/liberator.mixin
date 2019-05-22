(ns liberator-mixin.util
  (:import [java.util UUID]))

(defn random-uuid []
  (str (UUID/randomUUID)))
