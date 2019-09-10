(ns liberator-mixin.json.core
  (:require
    [clojure.string :refer [starts-with?]]

    [jason.core :as jason :refer [defcoders]]

    [camel-snake-kebab.core
     :refer [->camelCaseString
             ->kebab-case-keyword]])
  (:import
    [com.fasterxml.jackson.core JsonParseException]))

(def ^:private <-wire-json (jason/new-json-decoder))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (seq (re-find #"^application/(.+\+)?json" type))))

(defn- read-json [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      (try
        [true (<-wire-json body)]
        (catch JsonParseException _
          [false nil])))))

(def json-media-type "application/json")

(defn with-json-media-type []
  {:available-media-types [json-media-type]})

(defn with-body-parsed-as-json []
  {:initialize-context
   (fn [{:keys [request]}]
     (if-let [[valid? json] (read-json request)]
       (if valid?
         {:request {:body json}}
         {:malformed? true})))

   :malformed?
   (fn [{:keys [malformed?]}] malformed?)})

(defn with-json-mixin [_]
  [(with-json-media-type)
   (with-body-parsed-as-json)])
