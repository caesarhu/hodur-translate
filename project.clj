(defproject hodur-translate "0.1.2"
  :description "hodur plugins, postgres supported"
  :url "https://github.com/caesarhu/hodur-translate.git"
  :license {:name "Apache License, Version 2.0."
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" :clojars
                         :creds :gpg]
                        ["snapshots" :clojars
                         :creds :gpg]]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [duct/core "0.8.0"]
                 [duct/module.logging "0.5.0"]
                 [hodur/spec-schema "0.1.5"]
                 [datascript "1.0.1"]
                 [camel-snake-kebab "0.4.2"]
                 [cjsauer/disqualified "0.1.0"]
                 [com.rpl/specter "1.1.3"]
                 [metosin/spec-tools "0.10.4"]
                 [clojure.java-time "0.3.2"]
                 [mvxcvi/cljstyle "0.14.0"]
                 [funcool/datoteka "1.2.0"]]
  :plugins [[duct/lein-duct "0.12.1"]]
  :main ^:skip-aot hodur-translate.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :middleware     [lein-duct.plugin/middleware]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.2"]
                                   [hawk "0.2.11"]
                                   [eftest "0.5.9"]
                                   [aero "1.1.6"]]}})
