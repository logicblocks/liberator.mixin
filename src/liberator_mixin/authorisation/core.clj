(ns liberator-mixin.authorisation.core
  "Liberator mixin to authorise a request based on an access token"
  (:require [clojure.string :as str]
            [buddy.auth.http :as http]
            [buddy.sign.jwt :as jwt]
            [liberator-mixin.json.core :refer [with-json-media-type]]
            [clojure.string :as string])
  (:import [clojure.lang ExceptionInfo]))

(defn- parse-header
  [request token-name token-parser]
  (let [header (http/-get-header request "authorization")
        cases [(string/capitalize token-name)
               (string/lower-case token-name)
               (string/upper-case token-name)]
        pattern (re-pattern
                  (str "^(?:" (string/join "|" cases) ") (.+)$"))]
    (some->> header
      (re-find pattern)
      (second)
      (token-parser))))

(defn- to-error
  ([message]
   (to-error message {}))
  ([message data]
   {:authorised? false
    :error       {:message message
                  :data    data}}))

(defn- is-authorised? [token-claims claims]
  (if (empty? token-claims)
    {:authorised? true}
    (let [results
          (for [token-claim token-claims
                :let [key (first token-claim)
                      validator (second token-claim)
                      found (key claims)]]
            {:authorised? (and (some? found) (validator found))
             :error       {:message (format "Access token failed validation for %s." (name key))
                           :data    {:type :validation :cause key}}})]
      (reduce (fn [state curr]
                {:authorised? (and (:authorised? state) (:authorised? curr))
                 :error       (merge (:error state) (:error curr))})
        results))))

(defn- parse-token [token-claims token-key token-opts data]
  (try
    (let [claims (jwt/unsign data token-key token-opts)
          {:keys [authorised? error]} (is-authorised? token-claims claims)]
      {:identity    claims
       :authorised? authorised?
       :error       error})
    (catch ExceptionInfo e
      (to-error (ex-message e) (ex-data e)))))

(defn- missing-token [token]
  (to-error
    (format "Message does not contain a %s token." token)
    {:type :validation :cause :token}))

(defn with-jws-access-token
  "Returns a mixin that validates the jws access token ensure it includes the claims
  and that claim passes validation, finally it stores the authentication and authorisation
  state on the context under :identity

  The secret can be a function which is provided the JOSE header as its single
  param

  Takes token as an optional param that changes the type of token looked for
  (default is Bearer)

  Takes opts as an optional param that is used to validate the claims of the
  token (aud, iss, sub, exp, nbf, iat)

  This mixin should only be used once."
  []
  {:initialize-context (fn [{:keys [request resource]}]
                         (let [token-type (get resource :token-type (constantly "Bearer"))
                               token-options (get resource :token-options (constantly {}))
                               token-key (get resource :token-key)
                               token-claims (get resource :token-claims (constantly {}))
                               token-parser (get resource :token-parser identity)
                               token (parse-header request (token-type) token-parser)]
                           (if (some? token)
                             (parse-token (token-claims) (token-key) (token-options) token)
                             (missing-token (token-type)))))
   :authorized?        (fn [{:keys [authorised?]}] authorised?)})

(defn- error-to-status
  [{:keys [data]}]
  (let [{:keys [type cause]} data]
    (cond
      (and (= type :validation) (= cause :token)) 400
      (and (= type :validation) (= cause :scope)) 403
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

(defn with-www-authenticate
  "Returns a mixin populates the WWW-Authenticate error when the
  JWT is not authorised to access the protected endpoint.

  This mixin should only be used once."
  []
  {:handle-unauthorized
   (fn [{:keys [error]}]
     (liberator.representation/ring-response
       {:status  (error-to-status error)
        :headers {"WWW-Authenticate" (error-to-header error)}}))})