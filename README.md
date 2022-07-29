# liberator-mixin

An extension to liberator allowing for composable mixins.

## Install

Add the following to your `project.clj` file:

```clj
[b-social/liberator-mixin "0.0.60"]
```

## Documentation

* [API Docs](http://b-social.github.io/liberator-mixin)

## Usage

FIXME

## Contributing

### Testing

Run tests using:

```
lein test
```

### Releasing a new version

Prerequisites:
- You must have git configured with your GPG credentials (https://git-scm.com/book/en/v2/Git-Tools-Signing-Your-Work).
- You must have lein configured with your Clojars credentials (instructions: https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#gpg).
- You must be a member of the b-social group on Clojars.

You should then be able to run:
```
lein release
```

This will handle changing the versions everywhere, and adding the version tags.
It will deploy the latest to Clojars and push all the changes to GitHub.

## License

Copyright © 2018 B-Social Ltd.

Distributed under the terms of the 
[MIT License](http://opensource.org/licenses/MIT).
