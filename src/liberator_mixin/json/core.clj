(ns liberator-mixin.json.core
  (:require
    [clojure.string :refer [starts-with?]]

    [jason.core :refer [defcoders]]

    [liberator.representation :as r]
    [liberator-mixin.context.core :refer [with-attribute-in-context]])
  (:import
    [com.fasterxml.jackson.core JsonParseException]))

(declare
  ->wire-json
  <-wire-json)
(defcoders wire)

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (seq (re-find #"^application/(.+\+)?json" type))))

(defn- read-json-body [request decoder]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (decoder body-string)]
          (catch JsonParseException _
            [false nil]))))))

(defn- read-json-params [request decoder]
  (letfn [(parse-param [value]
            (try
              (if (coll? value)
                (into (empty value) (map parse-param value))
                (decoder value))
              (catch JsonParseException _
                value)))]
    (let [params (:params request)]
      (into {}
        (map (fn [[k v]] [k (parse-param v)])
          params)))))

(def json-media-type "application/json")

(defmethod r/render-map-generic json-media-type [data {:keys [json]}]
  ((get json :encoder ->wire-json) data))

(defmethod r/render-seq-generic json-media-type [data {:keys [json]}]
  ((get json :encoder ->wire-json) data))

(defn with-json-media-type []
  {:available-media-types [json-media-type]})

(defn with-json-decoder [decoder]
  (with-attribute-in-context :json {:decoder decoder}))

(defn with-json-encoder [encoder]
  (with-attribute-in-context :json {:encoder encoder}))

(defn with-body-parsed-as-json []
  {:initialize-context
   (fn [{:keys [request json]}]
     (let [decoder (get json :decoder <-wire-json)]
       (if-let [[valid? json] (read-json-body request decoder)]
         (if valid?
           {:request {:body json}}
           {:malformed? true}))))

   :malformed?
   (fn [{:keys [malformed?]}] malformed?)})

(defn with-params-parsed-as-json []
  {:initialize-context
   (fn [{:keys [request json]}]
     (let [decoder (get json :decoder <-wire-json)
           params (read-json-params request decoder)]
       {:request {:params (with-meta params {:replace true})}}))})

(defn with-json-mixin [dependencies]
  [(with-json-encoder (get-in dependencies [:json :encoder] ->wire-json))
   (with-json-decoder (get-in dependencies [:json :decoder] <-wire-json))
   (with-json-media-type)
   (with-body-parsed-as-json)
   (with-params-parsed-as-json)])
