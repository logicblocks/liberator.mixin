(defproject io.logicblocks/liberator.mixin.authorisation "0.1.0-RC10"
  :description "An authorisation mixin for liberator."

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

                 [buddy/buddy-auth]
                 [com.auth0/java-jwt]]

  :profiles {:shared      {:dependencies [[io.logicblocks/liberator.mixin.core]
                                          [io.logicblocks/liberator.mixin.json]

                                          [ring/ring-mock]

                                          [tick]]}
             :reveal      [:parent-reveal]
             :dev         [:parent-dev :shared]
             :unit        [:parent-unit :shared]}

  :test-paths ["test/unit"]
  :resource-paths [])
