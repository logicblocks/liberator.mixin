# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com)
and this project adheres to
[Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Forked from `b-social/liberator-mixin`.
- Rename library to `liberator.mixin`
- Rename `:routes` to `:router` in `liberator.mixin.hal` and
  `liberator.mixin.hypermedia` to reflect upstream changes in `hype`.
- Switch to a multimodule build with the core mixin logic implemented by 
  `liberator.mixin.core` and the specific mixins moved respectively to:
  - `liberator.mixin.authorisation`
  - `liberator.mixin.context`
  - `liberator.mixin.hal`
  - `liberator.mixin.hypermedia`
  - `liberator.mixin.json`
  - `liberator.mixin.logging`
  - `liberator.mixin.validation`
- The `liberator.mixin.logging` mixin now expects a `cartus.core/Logger` rather
  than implementing a custom protocol for logging. The `liberator.mixin.hal`
  mixin has also been updated to use `cartus.core` for logging.
- When logging exceptions in `liberator.mixin.hal/with-exception-handler`, the
  exception is first converted to a map using `clojure.core/Throwable->map` in
  order to retain detail.
- The resource key `:self` introduced as part of the 
  `liberator.mixin.hypermedia` mixin has been renamed to `:self-link` for 
  clarity and the corresponding `:self` entry added to context has been 
  renamed to `:self-link`. 
- The `is-` prefix on predicates in `liberator.mixin.core` has been dropped to
  follow convention.

### Added

- A new `SpecBackedValidator` has been added to `liberator.mixin.validation`
  allowing a spec to be used as a validator, along with a function 
  `spec-validator` to assist in construction of the validator.
- A new `MultiValidator` and supporting `combine` function has been added to
  `liberator.mixin.validation` allowing validators to be combined such that all
  validators must report valid in order for the combined to report valid and
  such that problems from all validators are combined in the combined validator.
- Two helpers, `resource-attribute-as-value` and `resource-attribute-as-fn`, for 
  fetching resource attributes from context, have been added in
  `liberator.mixin.util`.
  - `resource-attribute-as-value` fetches the attribute from the resource and 
    invokes it passing context, returning the result.
  - `resource-attribute-as-fn` fetches the attribute from the resource and
    partially applies it using the context, returning a function of the 
    remaining parameters
- 

### Fixed

- Fix issue where merging mixins with duplicated decisions e.g. `allowed?` would
  duplicate values in sequential collections of the context.

## [0.0.61] — 2023-01-10

### Added

- The `liberator-mixin.hal` namespace now includes a
  `with-method-not-allowed-handler` mixin for correctly handling method not
  allowed responses as a HAL resource.

### Changed

- The `with-hal-mixin` mixin now includes the `with-exception-handler`,
  `with-unauthorized-handler`, `with-forbidden-handler` and
  `with-method-not-allowed-handler` mixins.

## [0.0.60] — 2022-07-29

### Changed

- When a token is not found on the request but is required, a 401 response is
  now returned rather than a 400 response.
- The `with-jwt-scopes` mixin now allows specifying whether a token is required
  on the request via the `:token-required?` configuration option.

## [0.0.59] — 2022-06-30

### Added

- `with-bearer-token` now allows the `:token-type` configuration option to be a
  seq of accepted token types, with `nil` representing there being no token type
  identifier in the header value. All `:token-type`s are attempted when parsing
  a token header.

## [0.0.58] — 2022-06-28

### Added

- The `liberator-mixin.hal` namespace now includes a `with-unauthorized-handler`
  mixin for correctly handling unauthorized responses as a HAL resource.
- A `with-jwt-scopes` mixin has been added in
  `liberator-mixin.authorisation.unverified` allowing the scopes claim to be
  extracted to context but not verified.

## [0.0.57] — 2021-11-25

### Added

- The `with-bearer-token` mixin now allows the header to look in for a token
  to be specified via the `:token-header-name` configuration option, which
  defaults to "Authorization".

## [0.0.56] — 2020-09-23

### Changed

- The `:token-required?` configuration option now allows a map of methods to
  booleans to be used, such that requiring a token can be opted out of for
  certain methods.

## [0.0.55] — 2020-09-18

### Changed

- The `:token-missing` configuration option for `with-token-authorization`
  has been removed in favour of a new `:token-required?` configuration option,
  which when false will allow requests to proceed even if there is no token
  on the request.
- The `malformed?` and `allowed?` decisions for liberator have now been used
  rather than manually constructing responses within 
  `liberator-mixin.authorisation`.
- The `with-handle-unauthorized-token` mixin has been renamed to
  `with-www-authenticate-header`.
- Error responses are now rendered via `:as-response` rather than overriding
  `:handle-unauthorized`.
- The `ScopeValidator` now allows required scopes to be specified by request
  method.

## [0.0.54] — 2020-06-25

### Changed

- A number of dependencies have been upgraded.
- The `with-access-token` mixin has been renamed to `with-bearer-token`.
- The `with-jws-access-token` mixin has been renamed to
  `with-token-authorization`.
- The `with-www-authenticate` mixin has been renamed to
  `with-handle-unauthorized-token`.
- A new `:token-missing` configuration option has been added for use by
  `with-token-authorization` allowing the action to perform in the case of a
  missing token to be customises, defaulting to responding with an unauthorized
  response.

## [0.0.53] — 2020-06-23

### Added

- The `liberator-mixin.hal` namespace now includes a `with-forbidden-handler`
  mixin for correctly handling forbidden responses as a HAL resource.

## [0.0.52] — 2020-06-12

No observable change.

## [0.0.51] — 2020-06-08

### Changed

- The `with-jws-access-token` mixin now allows a seq of `ClaimValidator`
  instances to be provided via the `:token-validators` configuration option
  making the claim validator more general purpose and extensible and less
  opinionated.
- The `scope-validator` function has been replaced with a `ScopeValidator` type,
  implementing `ClaimValidator` which can be used in the aforementioned 
  `:token-validators` seq.

## [0.0.50] — 2020-06-02

### Added

- A `with-logger` mixin has been introduced in the `liberator-mixin.logging`
  namespace, allowing a `logger` to be set on the context for use by other
  mixins.

## [0.0.49] — 2020-05-13

### Fixed

- The `WWW-Authenticate` header was incorrectly reporting the underlying error.
  This has now been resolved.

### Changed

- The `with-www-authenticate` mixin now allows an `:error-body` to be set on
  the context, used as the body of the error response.

## [0.0.48] — 2020-03-04

### Added

- A `with-jws-access-token-mixin` has been added that sets up all parts of JWS
  access token management.

### Changed

- A `with-access-token` mixin has been pulled out of `with-jws-access-token`
  so that extracting and parsing the token can be performed independently
  without further verification.

## [0.0.47] — 2020-02-28

### Fixed

- Previously verifying multiple scopes failed. This is now resolved.

## [0.0.46] — 2020-02-28

### Added

- The `:token-key` option used by `with-jws-access-token` now accepts a function
  in addition to a value, which is called with `context` in order to retrieve
  the token key.

## [0.0.45] — 2020-02-28

### Changed

- The `liberator-mixin.jws-authorisation` namespace has been renamed to
  `liberator-mixin.authorisation`.
- The `with-jws-authorisation` mixin has been renamed to
  `with-jws-access-token`.
- The `with-jws-unauthorised` mixin has been renamed to `with-www-authenticate`.
- Configuration of `with-jws-access-token` has moved from mixin construction
  time to runtime by adding `:token-type`, `:token-options`, `:token-key`,
  `:token-claims` and `:token-parser` configuration options to the liberator
  handler map.
- A `scope-validator` function has been added to `liberator-mixin.authorisation`
  returning a claim validator to be used in the `:token-claims` configuration
  option.

## [0.0.44] — 2020-02-28

### Fixed

- Previously, the `with-jws-authorisation` mixin required the token type in the
  authorization header (e.g., "Bearer") to match specific casing. Now, the check
  is case-insensitive.

## [0.0.43] — 2020-02-24

### Changed

- The implementation of errors in `with-jws-unauthorised` has been tidied up to
  use a standard error format.

## [0.0.42] — 2020-02-19

### Added

- More tests of the "or" liberator decision changes have been introduced.

### Changed

- `with-jws-unauthorised-as-json` has been renamed to `with-jws-unauthorised`
  and no longer returns a JSON error body in the case of an authorisation
  failure, to comply with the JWT RFC.

## [0.0.41] — 2020-02-17

### Fixed

- Liberator decisions were incorrectly provided with a default value during
  merge resulting in erroneous decisions in the "or" cases. This has now been
  resolved.

## [0.0.40] — 2020-02-17

### Fixed

- Some liberator decisions were incorrectly being merged using "and" when they
  should have been merged using "or". This has now been resolved.

## [0.0.39] — 2020-02-13

### Fixed

- The error header returned from the `with-jws-unauthorised-as-json` mixin was
  invalid due to a typo. This has now been resolved.

## [0.0.38] — 2020-02-13

### Fixed

- The `with-jws-unauthorised-as-json` mixin was not correctly rendering error
  responses due to a missing representation media type. This has now been
  resolved.

## [0.0.37] — 2020-02-12

### Added

- The `with-jws-unauthorised-as-json` mixin now includes an OpenID conformant
  error header on the response in the case of a JWS authorisation failure.

## [0.0.36] — 2020-02-10

### Added

- A `with-jws-unauthorised-as-json` mixin has been added to the
  `liberator-mixin.jws-authorisation` namespace allowing a JWS authorisation
  failure to produce a descriptive JSON error response.

## [0.0.35] — 2020-02-07

### Added

- The `liberator-mixin.jws-authorisation` namespace has been introduced with a
  `with-jws-authorisation` mixin which allows verifying a JWS token on the
  request, including verifying the contents of the scope claim.

## [0.0.34] — 2019-12-04

### Added

- `liberator-mixin.context` has now been partially documented.
- The documentation for `liberator-mixin.hal`, `liberator-mixin.hypermedia` and
  `liberator-mixin.json` has been improved.
- The `jason` and `camel-snake-kebab` dependencies have been upgraded.

## [0.0.33] — 2019-10-22

### Added

- `liberator-mixin.context` has now been documented.
- The `liberator-mixin.context/with-attributes-in-context` mixin has been
  introduced, allowing multiple attributes to be added to context at once.

## [0.0.32] — 2019-10-22

### Added

- `liberator-mixin.core` has now been documented.
- `liberator-mixin.core/merge-decisions`
  and `liberator-mixin.core/merge-resource-definitions` now have increased test
  coverage.

### Fixed

- Issue with merging decisions that result in only context update maps.
- Issue with merging liberator decisions where when `left` is `false`,
  no merge would take place during
  `liberator-mixin.core/merge-resource-definitions`.

## [0.0.31] — 2019-09-18

### Changed

- The `liberator-mixin.validation` mixin now allows `nil` validators, so that
  the validator can be something like `(by-method :post (validator))` in a
  resource that supports both GET and POST requests.

## [0.0.30] — 2019-09-17

### Added

- A `FnBackedValidator` and factory function have been added to simplify
  building `Validator` instances from existing functions.

## [0.0.29] — 2019-09-17

### Changed

- The `liberator-mixin.validation` mixin now validates all known, validatable 
  methods by default.

## [0.0.28] — 2019-09-16

### Changed

- `jason` has been upgraded to the latest version.

## [0.0.27] — 2019-09-13

### Changed

- The test dependency on Clojure now uses version 1.10.1.

## [0.0.26] — 2019-09-13

### Changed

- Validators are now passed the context when they are constructed.

## [0.0.25] — 2019-09-12

### Changed

- Query strings are now always parsed as JSON when the JSON mixin is used,
  regardless of request Content-Type.

## [0.0.24] — 2019-09-12

### Added

- JSON parameter parsing is now added as part of the default JSON mixin
  contents.

## [0.0.23] — 2019-09-12

### Added

- JSON parameter parsing now supports multivalued parameters.

## [0.0.22] — 2019-09-11

### Added

- JSON parameter can now be parsed.

## [0.0.21] — 2019-09-11

### Added

- A context mixin has been introduced for adding arbitrary attributes to the
  context map.
- The JSON mixin now has support for using a custom encoder and decoder.

## [0.0.20] — 2019-09-11

### Changed

- The validator mixin now returns a map by default and looks for 
  error-representation functions on context to allow overriding and to decouple
  from HAL.

## [0.0.19] — 2019-09-11

### Changed

- Configuration merging now favours later definitions by placing them first in
  the resulting sequence.
- JSON encoding has been switched to use `jason` which provides default support
  for time types.

## [0.0.18] — 2019-09-10

### Removed

- URL generating functions from `liberator-mixin.hypermedia` have been replaced
  by `hype`, which now supersedes them in capability.

## [0.0.17] — 2019-09-10

### Changed

- Revert to `slurp`ing body before attempting JSON parse.

## [0.0.16] — 2019-09-10

### Fixed

- `jason` has been upgraded to a version that fixes a bug in the library.

## [0.0.15] — 2019-09-10

### Changed

- The `hal` and `json` mixins now use `jason` for JSON encoding and decoding
  instead of `cheshire`.

## [0.0.14] — 2019-09-09

### Added

- The test coverage for all merge functions has been improved.
- Configuration attributes such as `allowed-methods`,
  `available-media-types` etc can now be merged.

## [0.0.13] — 2019-06-17

### Changes

- `json->map` and `map->json` are now public.

## [0.0.12] — 2019-06-04

Unreleased.

## [0.0.11] — 2019-06-04

### Fixed

* Mixins can now be merged together regardless of whether they contain one or
  many fragments.

## [0.0.10] — 2019-05-22

### Changed

* When calculating validation issues, the whole context is now passed.

## [0.0.9] — 2019-05-22

No observable change.

## [0.0.8] — 2019-05-22

### Added

* Validation can now be performed on any method.

## [0.0.7] — 2019-05-22

### Changed

* The library is now called `liberator-mixin`.
* Many namespaces have been renamed/reorganised in preparation for splitting
  out into separate mixins.

## [0.0.6] — 2019-05-22

### Fixed

* Correctly handle unauthorised responses.

## [0.0.5] — 2019-05-22

No observable change.

## [0.0.4] — 2019-05-22

Released without _CHANGELOG.md_.

[0.0.4]: https://github.com/logicblocks/liberator.mixin/compare/0.0.4...0.0.4
[0.0.5]: https://github.com/logicblocks/liberator.mixin/compare/0.0.4...0.0.5
[0.0.6]: https://github.com/logicblocks/liberator.mixin/compare/0.0.5...0.0.6
[0.0.7]: https://github.com/logicblocks/liberator.mixin/compare/0.0.6...0.0.7
[0.0.9]: https://github.com/logicblocks/liberator.mixin/compare/0.0.7...0.0.9
[0.0.9]: https://github.com/logicblocks/liberator.mixin/compare/0.0.9...0.0.9
[0.0.9]: https://github.com/logicblocks/liberator.mixin/compare/0.0.9...0.0.9
[0.0.9]: https://github.com/logicblocks/liberator.mixin/compare/0.0.9...0.0.9
[0.0.9]: https://github.com/logicblocks/liberator.mixin/compare/0.0.9...0.0.9
[0.0.10]: https://github.com/logicblocks/liberator.mixin/compare/0.0.9...0.0.10
[0.0.10]: https://github.com/logicblocks/liberator.mixin/compare/0.0.10...0.0.10
[0.0.11]: https://github.com/logicblocks/liberator.mixin/compare/0.0.10...0.0.11
[0.0.14]: https://github.com/logicblocks/liberator.mixin/compare/0.0.11...0.0.14
[0.0.15]: https://github.com/logicblocks/liberator.mixin/compare/0.0.14...0.0.15
[0.0.16]: https://github.com/logicblocks/liberator.mixin/compare/0.0.15...0.0.16
[0.0.17]: https://github.com/logicblocks/liberator.mixin/compare/0.0.16...0.0.17
[0.0.18]: https://github.com/logicblocks/liberator.mixin/compare/0.0.17...0.0.18
[0.0.19]: https://github.com/logicblocks/liberator.mixin/compare/0.0.18...0.0.19
[0.0.20]: https://github.com/logicblocks/liberator.mixin/compare/0.0.19...0.0.20
[0.0.21]: https://github.com/logicblocks/liberator.mixin/compare/0.0.20...0.0.21
[0.0.22]: https://github.com/logicblocks/liberator.mixin/compare/0.0.21...0.0.22
[0.0.23]: https://github.com/logicblocks/liberator.mixin/compare/0.0.22...0.0.23
[0.0.24]: https://github.com/logicblocks/liberator.mixin/compare/0.0.23...0.0.24
[0.0.25]: https://github.com/logicblocks/liberator.mixin/compare/0.0.24...0.0.25
[0.0.26]: https://github.com/logicblocks/liberator.mixin/compare/0.0.25...0.0.26
[0.0.27]: https://github.com/logicblocks/liberator.mixin/compare/0.0.26...0.0.27
[0.0.28]: https://github.com/logicblocks/liberator.mixin/compare/0.0.27...0.0.28
[0.0.29]: https://github.com/logicblocks/liberator.mixin/compare/0.0.28...0.0.29
[0.0.30]: https://github.com/logicblocks/liberator.mixin/compare/0.0.29...0.0.30
[0.0.31]: https://github.com/logicblocks/liberator.mixin/compare/0.0.30...0.0.31
[0.0.32]: https://github.com/logicblocks/liberator.mixin/compare/0.0.31...0.0.32
[0.0.33]: https://github.com/logicblocks/liberator.mixin/compare/0.0.32...0.0.33
[0.0.34]: https://github.com/logicblocks/liberator.mixin/compare/0.0.33...0.0.34
[0.0.35]: https://github.com/logicblocks/liberator.mixin/compare/0.0.34...0.0.35
[0.0.36]: https://github.com/logicblocks/liberator.mixin/compare/0.0.35...0.0.36
[0.0.37]: https://github.com/logicblocks/liberator.mixin/compare/0.0.36...0.0.37
[0.0.38]: https://github.com/logicblocks/liberator.mixin/compare/0.0.37...0.0.38
[0.0.39]: https://github.com/logicblocks/liberator.mixin/compare/0.0.38...0.0.39
[0.0.40]: https://github.com/logicblocks/liberator.mixin/compare/0.0.39...0.0.40
[0.0.41]: https://github.com/logicblocks/liberator.mixin/compare/0.0.40...0.0.41
[0.0.42]: https://github.com/logicblocks/liberator.mixin/compare/0.0.41...0.0.42
[0.0.43]: https://github.com/logicblocks/liberator.mixin/compare/0.0.42...0.0.43
[0.0.44]: https://github.com/logicblocks/liberator.mixin/compare/0.0.43...0.0.44
[0.0.45]: https://github.com/logicblocks/liberator.mixin/compare/0.0.44...0.0.45
[0.0.46]: https://github.com/logicblocks/liberator.mixin/compare/0.0.45...0.0.46
[0.0.47]: https://github.com/logicblocks/liberator.mixin/compare/0.0.46...0.0.47
[0.0.48]: https://github.com/logicblocks/liberator.mixin/compare/0.0.47...0.0.48
[0.0.49]: https://github.com/logicblocks/liberator.mixin/compare/0.0.48...0.0.49
[0.0.50]: https://github.com/logicblocks/liberator.mixin/compare/0.0.49...0.0.50
[0.0.51]: https://github.com/logicblocks/liberator.mixin/compare/0.0.50...0.0.51
[0.0.52]: https://github.com/logicblocks/liberator.mixin/compare/0.0.51...0.0.52
[0.0.53]: https://github.com/logicblocks/liberator.mixin/compare/0.0.52...0.0.53
[0.0.54]: https://github.com/logicblocks/liberator.mixin/compare/0.0.53...0.0.54
[0.0.55]: https://github.com/logicblocks/liberator.mixin/compare/0.0.54...0.0.55
[0.0.56]: https://github.com/logicblocks/liberator.mixin/compare/0.0.55...0.0.56
[0.0.57]: https://github.com/logicblocks/liberator.mixin/compare/0.0.56...0.0.57
[0.0.58]: https://github.com/logicblocks/liberator.mixin/compare/0.0.57...0.0.58
[0.0.59]: https://github.com/logicblocks/liberator.mixin/compare/0.0.58...0.0.59
[0.0.60]: https://github.com/logicblocks/liberator.mixin/compare/0.0.59...0.0.60
[0.0.61]: https://github.com/logicblocks/liberator.mixin/compare/0.0.60...0.0.61
[Unreleased]: https://github.com/logicblocks/liberator-mixin/compare/0.0.61...HEAD
