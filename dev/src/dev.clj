(ns dev
  (:refer-clojure :exclude [test])
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
    [cjsauer.disqualified :refer [qualify-map unqualify-map]]
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.repl :refer [refresh]]
    [com.rpl.specter :as sp]
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [duct.core :as duct]
    [duct.core.repl :as duct-repl]
    [eftest.runner :as eftest]
    [fipp.edn :refer [pprint]]
    [hodur-translate.core :refer :all]
    [hodur-translate.data-spec :as ds]
    [hodur-translate.lacinia-schema :as ls]
    [hodur-translate.postgres-schema :as ps]
    [hodur-translate.postgres-sql :as sql]
    [hodur-translate.utils :as utils]
    [integrant.core :as ig]
    [integrant.repl :refer [clear halt go init prep reset]]
    [integrant.repl.state :refer [config system]]
    [java-time :as jt]
    [spec-tools.core :as st]))


(duct/load-hierarchy)


(defn read-config
  []
  (duct/read-config (io/resource "hodur_translate/config.edn")))


(defn test
  []
  (eftest/run-tests (eftest/find-tests "test")))


(def profiles
  [:duct.profile/dev :duct.profile/local])


(defn fit-spec
  ([spec value transformer]
   (let [res (st/conform spec value transformer)]
     (if (map? res)
       res
       false)))
  ([spec value]
   (fit-spec spec value st/strip-extra-keys-transformer)))


(defn meta-db
  []
  (init-schema (utils/read-schema "schema.edn")))


(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")


(when (io/resource "local.clj")
  (load "local"))


(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
