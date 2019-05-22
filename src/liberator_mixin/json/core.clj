(ns liberator-mixin.json.core
  (:require
    [clojure.string :refer [starts-with?]]

    [clj-time.coerce :as time-coerce]

    [cheshire.core :as json]
    [cheshire.generate :refer [add-encoder]]

    [camel-snake-kebab.core
     :refer [->camelCaseString
             ->snake_case_string
             ->kebab-case-keyword]]
    [camel-snake-kebab.extras
     :refer [transform-keys]]

    [liberator-mixin.core :as core])
  (:import
    [com.fasterxml.jackson.core JsonParseException]
    [com.fasterxml.jackson.core JsonGenerator]
    [org.joda.time DateTime]))

(defn- clj-time-encoder
  [data ^JsonGenerator json-generator]
  (let [^String date-time-as-string (time-coerce/to-string data)]
    (.writeString json-generator date-time-as-string)))

(defn- if-metadata
  [key-fn case-key-fn]
  (fn [key]
    (if (starts-with? (name key) "_")
      (key-fn key)
      (case-key-fn key))))

(add-encoder DateTime clj-time-encoder)

(defn- json->map [string options]
  (let [meta-key-fn (or (:meta-key-fn options) keyword)
        standard-key-fn (or (:standard-key-fn options) ->kebab-case-keyword)]
    (json/parse-string string
      (if-metadata meta-key-fn standard-key-fn))))

(defn- map->json [m options]
  (let [meta-key-fn (or (:meta-key-fn options) name)
        standard-key-fn (or (:standard-key-fn options) ->camelCaseString)]
    (json/generate-string m
      {:key-fn (if-metadata meta-key-fn standard-key-fn)
       :pretty true})))

(defn wire-json->map [string & {:as options}]
  (json->map string options))

(defn db-json->map [string & {:as options}]
  (json->map string options))

(defn map->wire-json [m & {:as options}]
  (map->json m (merge {:standard-key-fn ->camelCaseString} options)))

(defn map->db-json [m & {:as options}]
  (map->json m (merge {:standard-key-fn ->snake_case_string} options)))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (seq (re-find #"^application/(.+\+)?json" type))))

(defn- read-json [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (wire-json->map body-string)]
          (catch JsonParseException _
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

(defn with-json-mixin [_]
  (apply core/merge-resource-definitions
    [(with-json-media-type)
     (with-body-parsed-as-json)]))
