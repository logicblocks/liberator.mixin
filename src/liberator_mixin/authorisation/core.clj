(ns liberator-mixin.authorisation.core
  "Liberator mixin to authorise a request based on an access token"
  (:require [buddy.auth.http :as http]
            [buddy.sign.jwt :as jwt]
            [clojure.string :as string]
            [liberator.representation :as representation]))

(defprotocol ClaimValidator
  (validate
    [this ctx value]
    "Validate a claim.

    Params:
    * ctx - liberator context
    * value -  claim value
    
    Returns an array of:
    * valid?
    * error message"))

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

(defn- is-valid? [ctx validators claims]
  (do
    (doseq [^ClaimValidator validator validators
            :let [[valid? error] (validate validator ctx claims)
                  {:keys [message cause]
                   :or   {message "Access token failed validation."
                          cause   {:type :validation :cause :claims}}} error]]
      (when-not (true? valid?) (throw (ex-info message cause))))
    true))

(defn- parse-token [ctx validators key options token]
  (try
    (let [claims (jwt/unsign token key options)]
      {:identity    claims
       :authorised? (is-valid? ctx validators claims)})
    (catch Exception e
      {:authorised? false :exception e})))

(def missing-token
  {:exception (ex-info
                "Authorisation header does not contain a token."
                {:type :validation :cause :token})})

(defn with-access-token
  "Returns a mixin that extracts the access token from the authorisation header

  * token-type - the scheme under the authorisation header (default is Bearer)
  * token-parser - a function that performs parsing of the token before
  validation (optional)

  This mixin should only be used once."
  []
  {:initialize-context
   (fn [{:keys [request resource]}]
     (let [token-type (get resource :token-type (constantly "Bearer"))
           token-parser (get resource :token-parser identity)
           token (parse-header request (token-type) token-parser)]
       {:token token}))})

(defn with-jws-access-token
  "Returns a mixin that validates the jws access token ensure it includes the
  claims and that claim passes validation, finally it stores the authentication
  and authorisation state on the context under :identity

  This mixin assumes a token already on the context under :token

  * token-key - the secret can be a function which is provided the JOSE header
  as its single param
  * token-options - that is used to validate the standard claims of the
  token (aud, iss, sub, exp, nbf, iat) (optional)
  * token-validators - a array of ClaimValidators (optional)

  This mixin should only be used once."
  []
  {:authorized?
   (fn [{:keys [token resource] :as ctx}]
     (if (some? token)
       (let [{:keys [token-options token-key token-validators]
              :or   {token-options    (constantly {})
                     token-validators (constantly [])}} resource
             {:keys [authorised?] :as result}
             (parse-token ctx (token-validators) token-key (token-options) token)]
         [authorised? result])
       [false missing-token]))})

(defn- data-to-status
  [{:keys [type cause]}]
  (cond
    (and (= type :validation) (= cause :token)) 400
    (and (= type :validation) (= cause :claims)) 403
    (= type :validation) 401
    :else 500))

(defn- data-to-type
  [{:keys [type cause]}]
  (cond
    (and (= type :validation) (= cause :token)) "invalid_request"
    (and (= type :validation) (= cause :scope)) "insufficient_scope"
    (= type :validation) "invalid_token"
    :else "internal_server_error"))

(defn- error-to-header [data message]
  (str
    "Bearer,\n"
    "error=\"" (data-to-type data) "\",\n"
    "error_message=\"" message "\"\n"))

(defn with-www-authenticate
  "Returns a mixin that populates the WWW-Authenticate error when the
  request is not authorised to access the protected endpoint.

  This mixin should only be used once."
  []
  {:handle-unauthorized
   (fn [{:keys [^Exception exception error-body]}]
     (let [message (ex-message exception)
           data (ex-data exception)]
       (representation/ring-response
         error-body
         {:status  (data-to-status data)
          :headers {"WWW-Authenticate" (error-to-header data message)}})))})

(defn with-jws-access-token-mixin
  []
  [(with-access-token)
   (with-jws-access-token)
   (with-www-authenticate)])

(deftype ScopeValidator
  [required-scopes]
  ClaimValidator
  (validate [_ _ claims]
    (let [scope (:scope claims)]
      (if
        (and
          (some? scope)
          (every? (set (string/split scope #" ")) required-scopes))
        [true]
        [false {:message "Access token failed validation for scope."
                :cause   {:type :validation :cause :claims}}]))))

(defn ->ScopeValidator [required-scopes] (ScopeValidator. required-scopes))