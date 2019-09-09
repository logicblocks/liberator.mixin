(defproject b-social/liberator-mixin "0.0.14-SNAPSHOT"
  :description "A collection of sensible defaults for liberator microservices."
  :url "https://github.com/b-social/liberator-mixin"
  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[cheshire "5.9.0"]
                 [liberator "0.15.3"]
                 [halboy "5.1.0"]
                 [camel-snake-kebab "0.4.0"]
                 [clj-time "0.15.1"]
                 [bidi "2.1.6"]]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]
            [lein-eftest "0.5.8"]]
  :profiles {:shared {:dependencies [[org.clojure/clojure "1.10.0"]
                                     [ring/ring-mock "0.4.0"]
                                     [eftest "0.5.8"]]}
             :dev    [:shared]
             :test   [:shared]}
  :eftest {:multithread? false}
  :repl-options {:init-ns liberator-mixin.core}
  :deploy-repositories {"releases" {:url   "https://repo.clojars.org"
                                    :creds :gpg}}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["vcs" "push"]]
  :aliases {"test" ["eftest" ":all"]})
