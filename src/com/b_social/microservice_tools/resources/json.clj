(ns com.b-social.microservice-tools.resources.json
  (:require [com.b-social.microservice-tools.json :as json])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (seq (re-find #"^application/(.+\+)?json" type))))

(defn- read-json [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (json/wire-json->map body-string)]
          (catch JsonParseException ex
            [false nil]))))))

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