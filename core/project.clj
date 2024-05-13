(defproject io.logicblocks/liberator.mixin.core "0.1.0-RC15"
  :description "Functions for defining composable liberator mixins."

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

  :dependencies [[liberator]]

  :profiles {:shared {:dependencies
                      [[ring/ring-core]
                       [ring/ring-mock]]}
             :reveal [:parent-reveal]
             :dev    [:parent-dev :shared]
             :unit   [:parent-unit :shared]}

  :test-paths ["test/unit"]
  :resource-paths [])
