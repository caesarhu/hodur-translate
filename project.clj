(defproject caesarhu/hodur-translate "0.1.15"
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
                 [hodur/spec-schema "0.1.5"]
                 [datascript "1.0.1"]
                 [camel-snake-kebab "0.4.2"]
                 [cjsauer/disqualified "0.1.0"]
                 [com.rpl/specter "1.1.3"]
                 [metosin/spec-tools "0.10.4"]
                 [clojure.java-time "0.3.2"]
                 [mvxcvi/cljstyle "0.14.0"]
                 [funcool/datoteka "1.2.0"]
                 [honeysql "1.0.444"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [caesarhu/sql-formatter "0.1.0"]
                 [juxt/clip "0.20.0"]
                 [aero/aero "1.1.6"]
                 [metosin/malli "0.2.1"]]
  :main ^:skip-aot hodur-translate.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[seancorfield/next.jdbc "1.1.613"]
                                   [fipp "0.6.23"]
                                   [org.postgresql/postgresql "42.2.18"]
                                   [migratus "1.3.3"]
                                   ;[com.taoensso/timbre "5.1.0"]
                                   [com.fzakaria/slf4j-timbre "0.3.20"]
                                   [org.clojure/tools.namespace "1.1.0"]]}})
