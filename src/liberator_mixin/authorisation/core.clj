(ns liberator-mixin.authorisation.core
  "Liberator mixin to authorise a request based on an access token"
  (:require [buddy.auth.http :as http]
            [buddy.sign.jwt :as jwt]
            [clojure.string :as string]
            [liberator.representation :as r]))

(defprotocol ClaimValidator
  (validate
    [this ctx claims]
    "Validate a tokens claims.

    Params:
    * ctx - liberator context
    * claims - token claims

    Returns an array of:
    * valid?
    * error map containing message and cause metadata"))

(defn- parse-header
  [request token-name token-parser token-header-name]
  (let [header (http/-get-header request token-header-name)
        cases [(string/capitalize token-name)
               (string/lower-case token-name)
               (string/upper-case token-name)]
        pattern (re-pattern
                  (str "^(?:" (string/join "|" cases) ") (.+)$"))]
    (some->> header
      (re-find pattern)
      (second)
      (token-parser))))

(defn- is-valid?
  [ctx validators claims]
  (doseq [^ClaimValidator validator validators
          :let [[valid? error] (validate validator ctx claims)
                {:keys [message cause]
                 :or   {message "Access token failed validation."
                        cause   {:type :validation :cause :claims}}} error]]
    (when-not (true? valid?) (throw (ex-info message cause))))
  true)

(defn- token->identity
  [key options token]
  (try
    (let [claims (jwt/unsign token key options)]
      [true {:identity claims}])
    (catch Exception e
      [false {:www-authenticate {:message   (ex-message e)
                                 :error     "invalid_token"
                                 :exception e}}])))

(def missing-token
  {:www-authenticate {:message "Authorisation header does not contain a token."
                      :error   "invalid_request"}})

(defn with-bearer-token
  "Returns a mixin that extracts the access token from the authorisation header

  * token-header-name - the name of the header containing the token (defaults to \"authorization\")
  * token-type - the scheme under the authorisation header (default is Bearer)
  * token-parser - a function that performs parsing of the token before
  validation (optional)

  This mixin should only be used once."
  []
  {:initialize-context
   (fn [{:keys [request resource]}]
     (let [token-header-name (get resource :token-header-name (constantly "authorization"))
           token-type (get resource :token-type (constantly "Bearer"))
           token-parser (get resource :token-parser identity)
           token (parse-header request (token-type) token-parser (token-header-name))]
       {:token token}))})

(defn with-token-authorization
  "Returns a mixin that validates the jws access token ensure it includes the
  claims and that claim passes validation, finally it stores the authentication
  and authorisation state on the context under :identity

  This mixin assumes a token already on the context under :token

  * token-key - the secret can be a function which is provided the JOSE header
  as its single param
  * token-options - that is used to validate the standard claims of the
  token (aud, iss, sub, exp, nbf, iat) (optional)
  * token-validators - a array of ClaimValidators (optional)
  * token-required? - whether a token should be treated as mandatory (defaults to true)


  This mixin should only be used once."
  []
  {:malformed?
   (fn [{:keys [token resource request]}]
     (let [{:keys [token-required?]
            :or   {token-required? (constantly {:any true})}} resource
           method (:request-method request)
           token-required? (token-required?)
           token-required? (or (get token-required? method) (get token-required? :any))]
       (when
         (and (nil? token) (true? token-required?))
         missing-token)))
   :authorized?
   (fn [{:keys [token resource]}]
     (let [{:keys [token-options token-key]
            :or   {token-options (constantly {})}} resource]
       (if (some? token)
         (token->identity token-key (token-options) token)
         true)))
   :allowed?
   (fn [{:keys [identity resource] :as ctx}]
     (if (some? identity)
       (let [{:keys [token-validators]
              :or   {token-validators (constantly [])}} resource]
         (try
           (is-valid? ctx (token-validators) identity)
           (catch Exception e
             [false {:www-authenticate
                     {:message   (ex-message e)
                      :error     "insufficient_scope"
                      :exception e}}])))
       true))})

(defn- error->header
  [{:keys [error message]}]
  (str
    "Bearer,\n"
    "error=\"" error "\",\n"
    "error_message=\"" message "\"\n"))


(defn with-www-authenticate-header
  "Returns a mixin that populates the WWW-Authenticate header when the
  request is not allowed to access the protected endpoint.

  This mixin should only be used once."
  []
  {:as-response
   (fn [d {:keys [www-authenticate] :as ctx}]
     (-> (r/as-response d ctx)
       (assoc-in
         [:headers "WWW-Authenticate"]
         (error->header www-authenticate))))})

(defn with-jws-access-token-mixin
  []
  [(with-bearer-token)
   (with-token-authorization)
   (with-www-authenticate-header)])

(deftype ScopeValidator
  [required-scopes]
  ClaimValidator
  (validate [_ ctx claims]
    (let [method (get-in ctx [:request :request-method])]
      (if-let [required-scopes (or (get required-scopes method) (get required-scopes :any))]
        (let [scope (:scope claims)]
          (if
            (and
              (some? scope)
              (every? (set (string/split scope #" ")) required-scopes))
            [true]
            [false {:message "Access token failed validation for scope."
                    :cause   {:type :validation :cause :claims}}]))
        [true]))))
