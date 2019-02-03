(ns com.b-social.microservice-tools.data
  (:import [java.util UUID]))

(defn random-uuid []
  (str (UUID/randomUUID)))
