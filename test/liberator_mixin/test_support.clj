(ns liberator-mixin.test-support)

(def decision-names-or
  [:malformed?
   :can-post-to-gone?
   :conflict?
   :existed?
   :moved-permanently?
   :moved-temporarily?
   :multiple-representations?
   :post-redirect?
   :put-to-different-url?
   :respond-with-entity?
   :uri-too-long?])
(def decision-names-and
  [:accept-charset-exists?
   :accept-encoding-exists?
   :accept-exists?
   :accept-language-exists?
   :allowed?
   :authorized?
   :can-post-to-missing?
   :can-put-to-missing?
   :charset-available?
   :delete-enacted?
   :encoding-available?
   :etag-matches-for-if-match?
   :etag-matches-for-if-none?
   :exists?
   :if-match-exists?
   :if-match-star-exists-for-missing?
   :if-match-star?
   :if-modified-since-exists?
   :if-modified-since-valid-date?
   :if-none-match-exists?
   :if-none-match-star?
   :if-none-match?
   :if-unmodified-since-exists?
   :if-unmodified-since-valid-date?
   :is-options?
   :known-content-type?
   :known-method?
   :language-available?
   :media-type-available?
   :method-allowed?
   :method-delete?
   :method-patch?
   :method-post?
   :method-put?
   :modified-since?
   :new?
   :patch-enacted?
   :post-enacted?
   :post-to-existing?
   :post-to-gone?
   :post-to-missing?
   :processable?
   :put-enacted?
   :put-to-existing?
   :service-available?
   :unmodified-since?
   :valid-content-header?
   :valid-entity-length?])

(def handler-names
  [:handle-accepted
   :handle-conflict
   :handle-created
   :handle-exception
   :handle-forbidden
   :handle-gone
   :handle-malformed
   :handle-method-not-allowed
   :handle-moved-permanently
   :handle-moved-temporarily
   :handle-multiple-representations
   :handle-no-content
   :handle-not-acceptable
   :handle-not-found
   :handle-not-implemented
   :handle-not-modified
   :handle-ok
   :handle-options
   :handle-precondition-failed
   :handle-request-entity-too-large
   :handle-see-other
   :handle-service-not-available
   :handle-unauthorized
   :handle-unknown-method
   :handle-unprocessable-entity
   :handle-unsupported-media-type
   :handle-uri-too-long])

(def action-names
  [:initialize-context
   :delete!
   :patch!
   :post!
   :put!])

(def configuration-names
  [:allowed-methods
   :known-methods
   :available-media-types
   :available-charsets
   :available-encodings
   :available-languages
   :patch-content-types])
