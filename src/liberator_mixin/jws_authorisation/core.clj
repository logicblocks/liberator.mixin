(ns liberator-mixin.jws-authorisation.core
  "Liberator mixin to authorise a request based on the scope claim in a signed
  jwt"
  (:require [clojure.string :as str]
    [buddy.auth.http :as http]
    [buddy.sign.jwt :as jwt]
    [liberator-mixin.json.core :refer [with-json-media-type]])
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
    {:authorised? false
     :error       {:message message
                   :data    data}}))

(defn- is-authorised? [claims required-scopes]
  (if (empty? required-scopes)
    {:authorised? true}
    (if-let [scope (:scope claims)]
      (if-let [authorised (every? (parse-scopes scope) required-scopes)]
        {:authorised? authorised}
        (to-error
          (format "Scope claim does not contain required scopes (%s)."
            (str/join "," required-scopes))
          {:type :validation :cause :missing-scopes}))
      (to-error
        "Token does not contain scope claim."
        {:type :validation :cause :scope}))))

(defn- parse-token [scopes secret data opts]
  (try
    (let [claims (jwt/unsign data secret opts)
          {:keys [authorised? error]} (is-authorised? claims scopes)]
      {:identity claims
       :authorised? authorised?
       :error error})
    (catch ExceptionInfo e
      (to-error (ex-message e) (ex-data e)))))

(defn- missing-token [token]
  (to-error
    (format "Message does not contain a %s token." token)
    {:type :validation :cause :token}))

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
   :authorized?        (fn [{:keys [authorised?]}] authorised?)})

(defn- error-to-status
  [{:keys [data]}]
  (let [{:keys [type cause]} data]
    (cond
      (and (= type :validation) (= cause :token)) 400
      (and (= type :validation) (= cause :missing-scopes)) 403
      (= type :validation) 401
      :else 500)))

(defn- data-to-type
  [{:keys [type cause]}]
  (cond
    (and (= type :validation) (= cause :token)) "invalid_request"
    (and (= type :validation) (= cause :missing-scopes)) "insufficient_scope"
    (= type :validation) "invalid_token"
    :else 500))

(defn- error-to-header [{:keys [message data]}]
  (str
    "Bearer,\n"
    "error=\"" (data-to-type data) "\",\n"
    "error_message=\"" message "\"\n"))

(defn with-jws-unauthorised
  "Returns a mixin populates the WWW-Authenticate error when the
  JWT is not authorised to access the protected endpoint.

  This mixin should only be used once."
  []
  {:authorized?
   (fn [{:keys [authorised?]}] authorised?)
   :handle-unauthorized
   (fn [{:keys [error]}]
     (liberator.representation/ring-response
       {:status  (error-to-status error)
        :headers {"WWW-Authenticate" (error-to-header error)}}))})