(ns dev
  (:refer-clojure :exclude [test])
  (:require
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.tools.namespace.repl :refer [refresh]]
    [com.rpl.specter :as sp]
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [duct.core :as duct]
    [duct.core.repl :as duct-repl]
    [eftest.runner :as eftest]
    [fipp.edn :refer [pprint]]
    [hodur-translate.bimap :as bi]
    [hodur-translate.engine :as engine]
    [hodur-translate.meta-db :as meta-db :refer [meta-db]]
    [hodur-translate.spec-schema :as hodur-spec]
    [hodur-translate.translate :as tc]
    [hodur-translate.utils :as utils]
    [integrant.core :as ig]
    [integrant.repl :refer [clear halt go init prep reset]]
    [integrant.repl.state :refer [config system]]
    [cjsauer.disqualified :refer [qualify-map unqualify-map]]
    [clojure.spec.alpha :as s]
    [com.rpl.specter :as sp]
    [hodur-translate.datomic-schema :as datomic]
    [hodur-translate.data-spec :as ds]
    [spec-tools.core :as st]
    [java-time :as jt]))


(duct/load-hierarchy)

(defn read-config
  []
  (duct/read-config (io/resource "hodur_translate/config.edn")))


(defn test
  []
  (eftest/run-tests (eftest/find-tests "test")))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(def spec-schema (hodur-spec/schema meta-db {:prefix :hodur-spec}))

(def spec-test
  (->> spec-schema
       (map rest)
       (map #(apply hash-map %))
       (apply merge)))

(defn fit-spec
  ([spec value transformer]
   (st/conform spec value transformer))
  ([spec value]
   (fit-spec spec value st/strip-extra-keys-transformer)))


(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")


(when (io/resource "local.clj")
  (load "local"))


(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
