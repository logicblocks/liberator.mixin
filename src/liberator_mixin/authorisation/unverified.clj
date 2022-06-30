(ns liberator-mixin.authorisation.unverified
  (:require
    [clojure.string :as string]
    [clojure.data.json :as json])
  (:import [com.auth0.jwt JWT]
           [java.util Base64]))

(defn- get-jwt-payload [token]
  (let [jwt (.decodeJwt (new JWT) token)
        payload (.getPayload jwt)
        decoded-payload (-> (.decode (Base64/getDecoder) payload)
                          (String.)
                          (json/read-str))]
    decoded-payload))

(defn- scope? [entry] (= (key entry) "scope"))

(defn with-jwt-scopes []
  {:authorized?
   (fn [{:keys [token]}]
     (try
       (if (some? token)
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