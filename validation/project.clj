(defproject io.logicblocks/liberator.mixin.validation "0.1.0-RC3"
  :description "A validation mixin for liberator."

  :parent-project {:path    "../parent/project.clj"
                   :inherit [:scm
                             :url
                             :license
                             :plugins
                             [:profiles :parent-shared]
                             [:profiles :parent-reveal]
                             [:profiles :parent-dev]
                             [:profiles :parent-unit]
                             :deploy-repositories
                             :managed-dependencies
                             :cloverage
                             :bikeshed
                             :cljfmt
                             :eastwood]}

  :plugins [[lein-parent "0.3.8"]]

  :dependencies [[liberator]]

  :profiles {:shared {:dependencies [[io.logicblocks/liberator.mixin.core]
                                     [io.logicblocks/liberator.mixin.json]

                                     [io.logicblocks/jason]

                                     [camel-snake-kebab]

                                     [ring/ring-mock]]}
             :reveal [:parent-reveal]
             :dev    [:parent-dev :shared]
             :unit   [:parent-unit :shared]}

  :test-paths ["test/unit"]
  :resource-paths [])
