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
    {:authorised false
     :message    message
     :data       data}))

(defn- is-authorised? [claims required-scopes]
  (if (empty? required-scopes)
    {:authorised true}
    (if-let [scope (:scope claims)]
      (if-let [authorised (every? (parse-scopes scope) required-scopes)]
        {:authorised authorised}
        (to-error
          (format "Scope claim does not contain required scopes (%s)."
            (str/join "," required-scopes))
          {:type :validation :cause :missing-scopes}))
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
               (format "Message does not contain a %s token." token)
               {:type :validation :cause :token})})

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

(defn- cause-to-status
  [{:keys [type cause]}]
  (cond
    (and (= type :validation) (= cause :token)) 400
    (and (= type :validation) (= cause :missing-scopes)) 403
    (= type :validation) 401
    :else 500))

(defn- cause-to-type
  [{:keys [type cause]}]
  (cond
    (and (= type :validation) (= cause :token)) "invalid_request"
    (and (= type :validation) (= cause :missing-scopes)) "insufficient_scope"
    (= type :validation) "invalid_token"
    :else 500))

(defn- cause-to-error [message cause]
  (str
    "Bearer,\n"
    "error=\"" (cause-to-type cause) "\",n"
    "error_message=\"" message "\"\n"))

(defn- with-jws-unauthorised
  []
  {:authorized?
   (fn [{:keys [identity]}]
     (let [authorised? (:authorised identity)]
       (if (true? authorised?)
         true
         [false {:representation {:media-type "application/json"}}])))
   :handle-unauthorized
   (fn [{:keys [identity]}]
     (let [{:keys [message data]} identity]
       (liberator.representation/ring-response
         identity
         {:status  (cause-to-status data)
          :headers {"WWW-Authenticate" (cause-to-error message data)}})))})

(defn with-jws-unauthorised-as-json
  "Returns a mixin that handles jws authentication failures returning the
detailed error information as json"
  []
  [(with-json-media-type)
   (with-jws-unauthorised)])