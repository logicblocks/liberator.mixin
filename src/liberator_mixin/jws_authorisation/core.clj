(ns liberator-mixin.jws-authorisation.core
  "Liberator mixin to authorise a request based on the scope claim in a signed
  jwt"
  (:require [clojure.string :as str]
    [buddy.auth.http :as http]
    [buddy.sign.jwt :as jwt])
  (:import [clojure.lang ExceptionInfo]))

(defn- parse-scopes [scope]
  (set (str/split scope #" ")))

(defn- parse-header
  [request token-name]
  (some->> (http/-get-header request "authorization")
    (re-find (re-pattern (str "^" token-name " (.+)$")))
    (second)))

(defn- to-error
  ([message]
    (to-error message {}))
  ([message data]
    {:authorised false
     :message    message
     :data       data}))

(defn- is-authorised? [claims required-scopes]
  (if (empty? required-scopes)
    {:authorised true}
    (if (some? claims)
      (if-let [scope (:scope claims)]
        {:authorised (every? (parse-scopes scope) required-scopes)}
        (to-error
          "Scope claim does not contain required scopes."
          {:type :authorisation :cause :missing-scopes}))
      (to-error
        "Token does not contain scope claim."
        {:type :validation :cause :scope}))))

(defn- parse-token [scopes secret data opts]
  (try
    (let [claims (jwt/unsign data secret opts) authorisation (is-authorised? claims scopes)]
      {:identity {:claims     claims
                  :authorised (:authorised authorisation)
                  :message    (:message authorisation)
                  :data       (:data authorisation)}})
    (catch ExceptionInfo e
      {:identity (to-error (ex-message e) (ex-data e))})))

(defn- missing-token [token]
  {:identity (to-error
               (format "Message does not contain a %s token." token))})

(defn with-jws-authorisation
  "Returns a mixin that validates the jws token ensure it includes the scope
  claim and that claim has the required scope, finally it stores the
  authentication and authorisation state on the context under :identity

  The secret can be a function which is provided the JOSE header as its single
  param

  Takes token as an optional param that changes the type of token looked for
  (default is Bearer)

  Takes opts as an optional param that is used to validate the claims of the
  token (aud, iss, sub, exp, nbf, iat)

  This mixin should only be used once."
  [scopes secret & {:keys [token opts] :or {token "Bearer" opts {}}}]
  {:initialize-context (fn [{:keys [request]}]
                         (let [data (parse-header request token)]
                           (if (some? data)
                             (parse-token scopes secret data opts)
                             (missing-token token))))
   :authorized?        (fn [{:keys [identity]}]
                         (:authorised identity))})
