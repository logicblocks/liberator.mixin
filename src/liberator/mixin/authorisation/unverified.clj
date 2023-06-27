(ns liberator.mixin.authorisation.unverified
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
   (fn [{:keys [token resource request]}]
     (let [{:keys [token-required?]
            :or   {token-required? (constantly true)}} resource
           method (:request-method request)
           token-required? (token-required?)
           token-required? (or
                             (true? token-required?)
                             (get token-required? method)
                             (get token-required? :any))]

       (cond
         (some? token)
         (try
           (let [decoded-payload (get-jwt-payload token)
                 scope-string (->>
                                (filter scope? decoded-payload)
                                (first)
                                (val))

                 scopes (set (string/split scope-string #" "))]
             [true {:scopes scopes}])
           (catch Exception e
             [false {:www-authenticate {:message (ex-message e)
                                        :error   "invalid_token"}}]))

         (true? token-required?)
         [false {:www-authenticate {:message "No x-auth-jwt token"
                                    :error   "invalid_token"}}]

         :default
         true)))})