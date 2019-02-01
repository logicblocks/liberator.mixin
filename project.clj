(defproject microservice-tools "0.0.1"
  :description "A collection of sensible defaults for liberator microservices"
  :url "https://github.com/b-social/microservice-tools"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.8.1"]
                 [liberator "0.15.2"]
                 [halboy "4.0.1"]
                 [camel-snake-kebab "0.4.0"]
                 [clj-time "0.15.1"]
                 [bidi "2.1.4"]]

  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]]}}

  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "s/\\\\[microservice-tools \"[0-9.]*\"\\\\]/[microservice-tools \"${:version}\"]/" "README.md"]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
