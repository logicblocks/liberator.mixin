# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com)
and this project adheres to 
[Semantic Versioning](http://semver.org/spec/v2.0.0.html).


## [Unreleased]

## [0.0.45] — 2020-02-28

## [0.0.44] — 2020-02-28

## [0.0.43] — 2020-02-24

## [0.0.42] — 2020-02-19

## [0.0.41] — 2020-02-17

## [0.0.40] — 2020-02-17

## [0.0.39] — 2020-02-13

## [0.0.38] — 2020-02-13

## [0.0.37] — 2020-02-12

## [0.0.36] — 2020-02-10

## [0.0.35] — 2020-02-07

## [0.0.34] — 2019-12-04

## [0.0.33] — 2019-10-22
### Added
- Documentation to `liberator-mixin.context`. 
- `liberator-mixin.context/with-attributes-in-context`.

## [0.0.32] — 2019-10-22
### Added
- Documentation to `liberator-mixin.core`.
- More test coverage to `liberator-mixin.core/merge-decisions`
  and `liberator-mixin.core/merge-resource-definitions`.

### Fixed
- Issue with merging decisions that result in only context update maps.
- Issue with merging liberator decisions where when `left` is `false`,
  no merge would take place during 
  `liberator-mixin.core/merge-resource-definitions`. 

## [0.0.31] — 2019-09-18
### Changed
- Allow `nil` validators in `liberator-mixin.validation` so that validator can
  be something like `(by-method :post (validator))` in a resource that supports 
  both GET and POST requests.

## [0.0.30] — 2019-09-17
### Added
- `FnBackedValidator` and factory function to simplify building `Validator`
  instances from existing functions. 

## [0.0.29] — 2019-09-17
### Changed
- Validate all known, validatable methods by default in 
  `liberator-mixin.validation`.

## [0.0.28] — 2019-09-16
### Changed
- Update jason to latest version.

## [0.0.27] — 2019-09-13
### Changed
- Update Clojure test dependency to 1.10.1.

## [0.0.26] — 2019-09-13
### Changed
- Validator constructor is now invoked with context.

## [0.0.25] — 2019-09-12
### Changed
- Always parse query string as JSON when JSON mixin used, regardless of request
  Content-Type.

## [0.0.24] — 2019-09-12
### Added
- JSON parameter parsing as part of the default JSON mixin.

## [0.0.23] — 2019-09-12
### Added
- JSON parameter parsing for multi-valued parameters.

## [0.0.22] — 2019-09-11
### Added
- JSON parameter parsing.

## [0.0.21] — 2019-09-11
### Added
- Context mixin for adding arbitrary attributes to the context map.
- Support for custom encoder and decoder in JSON mixin.

## [0.0.20] — 2019-09-11
### Changed
- Validator mixin to return map by default and look for error-representation
  function on context to allow overriding and to decouple from HAL. 

## [0.0.19] — 2019-09-11
### Changed
- Configuration merging to favour later definitions by placing them first in
  the resulting sequence.
- JSON encoding to use `jason` to provide default support for time types.

## [0.0.18] — 2019-09-10
### Removed
- URL generating functions from `liberator-mixin.hypermedia`. `hype` now
  supersedes them.

## [0.0.17] — 2019-09-10
### Changed
- Revert to `slurp`ing body before attempting JSON parse.

## [0.0.16] — 2019-09-10
### Fixed
- Bug in `jason` library.

## [0.0.15] — 2019-09-10

## [0.0.14] — 2019-09-09
### Added
- Test coverage for all merge functions.
- Configuration merging for attributes such as `allowed-methods`, 
  `available-media-types` etc.

## [0.0.12] — 2019-06-17
### Changes
- `json->map` and `map->json` are now public.

## [0.0.11] — 2019-06-04

## [0.0.10] — 2019-05-22

## [0.0.10] — 2019-05-22

## [0.0.9] — 2019-05-22

## [0.0.9] — 2019-05-22

## [0.0.9] — 2019-05-22

## [0.0.9] — 2019-05-22

## [0.0.9] — 2019-05-22

## [0.0.7] — 2019-05-22

## [0.0.6] — 2019-05-22

## [0.0.5] — 2019-05-22

## [0.0.4] — 2019-05-22
Released without _CHANGELOG.md_.

[0.0.4]: https://github.com/b-social/liberator-mixin/compare/0.0.4...0.0.4
[0.0.5]: https://github.com/b-social/liberator-mixin/compare/0.0.4...0.0.5
[0.0.6]: https://github.com/b-social/liberator-mixin/compare/0.0.5...0.0.6
[0.0.7]: https://github.com/b-social/liberator-mixin/compare/0.0.6...0.0.7
[0.0.9]: https://github.com/b-social/liberator-mixin/compare/0.0.7...0.0.9
[0.0.9]: https://github.com/b-social/liberator-mixin/compare/0.0.9...0.0.9
[0.0.9]: https://github.com/b-social/liberator-mixin/compare/0.0.9...0.0.9
[0.0.9]: https://github.com/b-social/liberator-mixin/compare/0.0.9...0.0.9
[0.0.9]: https://github.com/b-social/liberator-mixin/compare/0.0.9...0.0.9
[0.0.10]: https://github.com/b-social/liberator-mixin/compare/0.0.9...0.0.10
[0.0.10]: https://github.com/b-social/liberator-mixin/compare/0.0.10...0.0.10
[0.0.11]: https://github.com/b-social/liberator-mixin/compare/0.0.10...0.0.11
[0.0.14]: https://github.com/b-social/liberator-mixin/compare/0.0.11...0.0.14
[0.0.15]: https://github.com/b-social/liberator-mixin/compare/0.0.14...0.0.15
[0.0.16]: https://github.com/b-social/liberator-mixin/compare/0.0.15...0.0.16
[0.0.17]: https://github.com/b-social/liberator-mixin/compare/0.0.16...0.0.17
[0.0.18]: https://github.com/b-social/liberator-mixin/compare/0.0.17...0.0.18
[0.0.19]: https://github.com/b-social/liberator-mixin/compare/0.0.18...0.0.19
[0.0.20]: https://github.com/b-social/liberator-mixin/compare/0.0.19...0.0.20
[0.0.21]: https://github.com/b-social/liberator-mixin/compare/0.0.20...0.0.21
[0.0.22]: https://github.com/b-social/liberator-mixin/compare/0.0.21...0.0.22
[0.0.23]: https://github.com/b-social/liberator-mixin/compare/0.0.22...0.0.23
[0.0.24]: https://github.com/b-social/liberator-mixin/compare/0.0.23...0.0.24
[0.0.25]: https://github.com/b-social/liberator-mixin/compare/0.0.24...0.0.25
[0.0.26]: https://github.com/b-social/liberator-mixin/compare/0.0.25...0.0.26
[0.0.27]: https://github.com/b-social/liberator-mixin/compare/0.0.26...0.0.27
[0.0.28]: https://github.com/b-social/liberator-mixin/compare/0.0.27...0.0.28
[0.0.29]: https://github.com/b-social/liberator-mixin/compare/0.0.28...0.0.29
[0.0.30]: https://github.com/b-social/liberator-mixin/compare/0.0.29...0.0.30
[0.0.31]: https://github.com/b-social/liberator-mixin/compare/0.0.30...0.0.31
[0.0.32]: https://github.com/b-social/liberator-mixin/compare/0.0.31...0.0.32
[0.0.33]: https://github.com/b-social/liberator-mixin/compare/0.0.32...0.0.33
[0.0.34]: https://github.com/b-social/liberator-mixin/compare/0.0.33...0.0.34
[0.0.35]: https://github.com/b-social/liberator-mixin/compare/0.0.34...0.0.35
[0.0.36]: https://github.com/b-social/liberator-mixin/compare/0.0.35...0.0.36
[0.0.37]: https://github.com/b-social/liberator-mixin/compare/0.0.36...0.0.37
[0.0.38]: https://github.com/b-social/liberator-mixin/compare/0.0.37...0.0.38
[0.0.39]: https://github.com/b-social/liberator-mixin/compare/0.0.38...0.0.39
[0.0.40]: https://github.com/b-social/liberator-mixin/compare/0.0.39...0.0.40
[0.0.41]: https://github.com/b-social/liberator-mixin/compare/0.0.40...0.0.41
[0.0.42]: https://github.com/b-social/liberator-mixin/compare/0.0.41...0.0.42
[0.0.43]: https://github.com/b-social/liberator-mixin/compare/0.0.42...0.0.43
[0.0.44]: https://github.com/b-social/liberator-mixin/compare/0.0.43...0.0.44
[0.0.45]: https://github.com/b-social/liberator-mixin/compare/0.0.44...0.0.45
[Unreleased]: https://github.com/b-social/liberator-mixin/compare/0.0.45...HEAD
