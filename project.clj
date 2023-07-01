(defproject io.logicblocks/liberator.mixin "0.1.0-RC1"
  :description "Extensions for liberator allowing for composable mixins."

  :parent-project {:path    "parent/project.clj"
                   :inherit [:scm
                             :url
                             :license
                             :plugins
                             [:profiles :parent-shared]
                             :deploy-repositories
                             :managed-dependencies]}

  :plugins [[io.logicblocks/lein-interpolate "0.1.1-RC2"]
            [lein-parent "0.3.9"]
            [lein-sub "0.3.0"]
            [lein-changelog "0.3.2"]
            [lein-codox "0.10.8"]]

  :sub ["parent"
        "core"
        "context"
        "logging"
        "json"
        "authorisation"
        "hypermedia"
        "hal"
        "validation"
        "."]

  :dependencies [[io.logicblocks/liberator.mixin.core]
                 [io.logicblocks/liberator.mixin.authorisation]
                 [io.logicblocks/liberator.mixin.context]
                 [io.logicblocks/liberator.mixin.hal]
                 [io.logicblocks/liberator.mixin.hypermedia]
                 [io.logicblocks/liberator.mixin.json]
                 [io.logicblocks/liberator.mixin.logging]
                 [io.logicblocks/liberator.mixin.validation]]

  :profiles
  {:unit
   {:aliases {"eftest"
              ["sub"
               "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
               "with-profile" "unit"
               "eftest"]}}

   :codox
   [:parent-shared
    {:dependencies [[io.logicblocks/liberator.mixin.core :project/version]

                    [tick]]
     :source-paths ["core/src"
                    "authorisation/src"
                    "context/src"
                    "hal/src"
                    "hypermedia/src"
                    "json/src"
                    "logging/src"
                    "validation/src"]}]

   :prerelease
   {:release-tasks
    [
     ["vcs" "assert-committed"]
     ["sub" "change" "version" "leiningen.release/bump-version" "rc"]
     ["sub" "change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["sub" "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation:." "deploy"]]}

   :release
   {:release-tasks
    [["vcs" "assert-committed"]
     ["sub" "change" "version" "leiningen.release/bump-version" "release"]
     ["sub" "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation:." "install"]
     ["changelog" "release"]
     ["shell" "sed" "-E" "-i.bak" "s/liberator\\.mixin\\.(.+) \"[0-9]+\\.[0-9]+\\.[0-9]+\"/liberator\\.mixin.\\\\1 \"${:version}\"/g" "README.md"]
     ["shell" "rm" "-f" "README.md.bak"]
     ["shell" "sed" "-E" "-i.bak" "s/liberator\\.mixin\\.(.+) \"[0-9]+\\.[0-9]+\\.[0-9]+\"/liberator\\.mixin.\\\\1 \"${:version}\"/g" "docs/01-getting-started.md"]
     ["shell" "sed" "-E" "-i.bak" "s/liberator\\.mixin\\.(.+) \"[0-9]+\\.[0-9]+\\.[0-9]+\"/liberator\\.mixin.\\\\1 \"${:version}\"/g" "docs/02-check-functions.md"]
     ["shell" "rm" "-f" "docs/01-getting-started.md.bak"]
     ["shell" "rm" "-f" "docs/02-check-functions.md.bak"]
     ["codox"]
     ["shell" "git" "add" "."]
     ["vcs" "commit" "Release version %s [skip ci]"]
     ["vcs" "tag"]
     ["sub" "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation:." "deploy"]
     ["sub" "change" "version" "leiningen.release/bump-version" "patch"]
     ["sub" "change" "version" "leiningen.release/bump-version" "rc"]
     ["sub" "change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["vcs" "push"]]}}

  :source-paths []
  :test-paths []
  :resource-paths []

  :codox
  {:namespaces  [#"^liberator\.mixin\."]
   :metadata    {:doc/format :markdown}
   :output-path "docs"
   :doc-paths   ["docs"]
   :source-uri  "https://github.com/logicblocks/liberator.mixin/blob/{version}/{filepath}#L{line}"}

  :aliases {"install"
            ["do"
             ["sub"
              "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
              "install"]
             ["install"]]

            "eastwood"
            ["sub"
             "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
             "eastwood"]

            "cljfmt"
            ["sub"
             "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
             "cljfmt"]

            "kibit"
            ["sub"
             "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
             "kibit"]

            "check"
            ["sub"
             "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
             "check"]

            "bikeshed"
            ["sub"
             "-s" "core:context:logging:json:authorisation:hypermedia:hal:validation"
             "bikeshed"]})
