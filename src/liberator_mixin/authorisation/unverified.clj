(ns liberator-mixin.authorisation.unverified
  (:require
    [clojure.string :as string]
    [clojure.data.json :as json])
  (:import [com.auth0.jwt JWT]
           [java.util Base64]))

(defn- get-jwt-payload [token]
  (let [token-string (second (string/split token #" "))
        jwt (.decodeJwt (new JWT) token-string)
        payload (.getPayload jwt)
        decoded-payload (-> (.decode (Base64/getDecoder) payload)
                          (String.)
                          (json/read-str))]
    decoded-payload))

(defn- scope? [entry] (= (key entry) "scope"))

(defn with-jwt-scopes []
  {:authorized?
   (fn [{:keys [request]}]
     (try
       (if-let [token (get-in request [:headers "x-auth-jwt"])]
         (let [
               decoded-payload (get-jwt-payload token)
               scope-string (->>
                              (filter scope? decoded-payload)
                              (first)
                              (val))

               scopes (set (string/split scope-string #" "))]
           [true {:scopes scopes}])
         [false {:www-authenticate {:message   "No x-auth-jwt token"
                                    :error     "invalid_token"}}])
       (catch Exception e
         [false {:www-authenticate {:message   (ex-message e)
                                    :error     "invalid_token"}}])))})