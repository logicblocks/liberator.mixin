(defproject io.logicblocks/liberator.mixin.json "0.1.0-RC14"
  :description "A JSON mixin for liberator."

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

  :plugins [[lein-parent "0.3.9"]]

  :dependencies [[liberator]

                 [io.logicblocks/jason]

                 [io.logicblocks/liberator.mixin.context]]

  :profiles {:shared {:dependencies [[io.logicblocks/liberator.mixin.core]
                                     [io.logicblocks/liberator.mixin.json]

                                     [camel-snake-kebab]

                                     [ring/ring-core]
                                     [ring/ring-mock]]}
             :reveal [:parent-reveal]
             :dev    [:parent-dev :shared]
             :unit   [:parent-unit :shared]}

  :test-paths ["test/unit"]
  :resource-paths [])
