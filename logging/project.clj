(defproject io.logicblocks/liberator.mixin.logging "0.1.0-RC20"
  :description "A logging mixin for liberator."

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

  :dependencies [[io.logicblocks/cartus.core]
                 [io.logicblocks/cartus.null]]

  :profiles {:shared {:dependencies [[io.logicblocks/liberator.mixin.core]

                                     [io.logicblocks/cartus.test]

                                     [io.logicblocks/jason]

                                     [ring/ring-core]
                                     [ring/ring-mock]]}
             :reveal [:parent-reveal]
             :dev    [:parent-dev :shared]
             :unit   [:parent-unit :shared]}

  :test-paths ["test/unit"]
  :resource-paths [])
