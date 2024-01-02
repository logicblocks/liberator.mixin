(defproject io.logicblocks/liberator.mixin.hal "0.1.0-RC7"
  :description "A HAL mixin for liberator."

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

                 [io.logicblocks/cartus.core]

                 [io.logicblocks/jason]
                 [io.logicblocks/halboy]
                 [io.logicblocks/hype]]

  :profiles {:shared {:dependencies [[io.logicblocks/liberator.mixin.core]
                                     [io.logicblocks/liberator.mixin.json]
                                     [io.logicblocks/liberator.mixin.hypermedia]

                                     [io.logicblocks/cartus.test]

                                     [camel-snake-kebab]

                                     [ring/ring-mock]]}
             :reveal [:parent-reveal]
             :dev    [:parent-dev :shared]
             :unit   [:parent-unit :shared]}

  :test-paths ["test/unit"]
  :resource-paths [])
