(defproject io.logicblocks/liberator.mixin.parent "0.1.0-RC15"
  :scm {:dir  "."
        :name "git"
        :url  "https://github.com/logicblocks/liberator.mixin"}

  :url "https://github.com/logicblocks/liberator.mixin"

  :license
  {:name "The MIT License"
   :url  "https://opensource.org/licenses/MIT"}

  :plugins [[io.logicblocks/lein-interpolate "0.1.1-RC3"]
            [jonase/eastwood "1.4.2"]
            [lein-ancient "0.7.0"]
            [lein-bikeshed "0.5.2"]
            [lein-cljfmt "0.9.2"]
            [lein-cloverage "1.2.4"]
            [lein-cprint "1.3.3"]
            [lein-eftest "0.6.0"]
            [lein-kibit "0.1.8"]
            [lein-shell "0.5.0"]
            [fipp "0.6.26"]]

  :deploy-repositories
  {"releases"  {:url "https://repo.clojars.org" :creds :gpg}
   "snapshots" {:url "https://repo.clojars.org" :creds :gpg}}

  :managed-dependencies
  [[org.clojure/clojure "1.11.1"]

   [io.logicblocks/liberator.mixin.core :project/version]
   [io.logicblocks/liberator.mixin.authorisation :project/version]
   [io.logicblocks/liberator.mixin.context :project/version]
   [io.logicblocks/liberator.mixin.hal :project/version]
   [io.logicblocks/liberator.mixin.hypermedia :project/version]
   [io.logicblocks/liberator.mixin.json :project/version]
   [io.logicblocks/liberator.mixin.logging :project/version]
   [io.logicblocks/liberator.mixin.validation :project/version]

   [io.logicblocks/jason "1.0.0"]
   [io.logicblocks/halboy "6.0.0"]
   [io.logicblocks/hype "2.0.0"]
   [io.logicblocks/spec.validate "0.2.0-RC19"]

   [io.logicblocks/cartus.core "0.1.18"]
   [io.logicblocks/cartus.test "0.1.18"]
   [io.logicblocks/cartus.null "0.1.18"]

   [liberator "0.15.3"]

   [metosin/spec-tools "0.10.6"]

   [buddy/buddy-auth "3.0.323"]

   [com.auth0/java-jwt "4.4.0"]

   [camel-snake-kebab "0.4.3"]

   [ring/ring-core "1.10.0"]
   [ring/ring-mock "0.4.0"]

   [tick "0.7.5"]

   [eftest "0.6.0"]]

  :profiles
  {:parent-shared
   ^{:pom-scope :test}
   {:dependencies [[org.clojure/clojure]

                   [eftest]]}

   :parent-reveal
   [:parent-shared
    {:dependencies [[vlaaad/reveal]]
     :repl-options {:nrepl-middleware [vlaaad.reveal.nrepl/middleware]}
     :jvm-opts     ["-Dvlaaad.reveal.prefs={:theme :light :font-family \"FiraCode Nerd Font Mono\" :font-size 13}"]}]

   :parent-dev
   ^{:pom-scope :test}
   [:parent-shared
    {:source-paths ["dev"]}]

   :parent-unit
   [:parent-shared {:test-paths ^:replace ["test/unit"]}]}

  :source-paths []
  :test-paths []
  :resource-paths []

  :cloverage
  {:ns-exclude-regex [#"^user"]}

  :bikeshed
  {:name-collisions false
   :long-lines      false}

  :cljfmt
  {:indents {#".*"     [[:inner 0]]
             defrecord [[:block 1] [:inner 1]]
             deftype   [[:block 1] [:inner 1]]}}

  :eastwood
  {:config-files
   [~(str (System/getProperty "user.dir") "/config/linter.clj")]})
