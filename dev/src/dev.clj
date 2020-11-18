(ns dev
  (:refer-clojure :exclude [test])
  (:require
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
    [hodur-translate.bimap :as bi]
    [hodur-translate.data-spec :as ds]
    [hodur-translate.postgres-schema :as postgres]
    [hodur-translate.engine :as engine]
    [hodur-translate.meta-db :as meta-db :refer [meta-db]]
    [hodur-translate.spec-schema :as hodur-spec]
    [hodur-translate.translate :as tc]
    [hodur-translate.utils :as utils]
    [integrant.core :as ig]
    [integrant.repl :refer [clear halt go init prep reset]]
    [integrant.repl.state :refer [config system]]
    [java-time :as jt]
    [spec-tools.core :as st]
    [com.rpl.specter :as sp]))


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

(defn get-all-list-items-id [conn]
  (let [eids (-> (d/q '[:find [?e ...]
                        :where
                        [?e :type/nature :user]
                        (not [?e :type/interface true])
                        (not [?e :type/union true])
                        [?e :type/name "all-list"]]
                      @conn)
                 vec flatten)
        selector '[* {:field/_parent
                      [* {:field/type [*]}]}]
        types (->> eids
                   (d/pull-many @conn selector)
                   (sort-by :type/name))]
    types))

(def query
  '[:find [?id ...]
    :where
    [?e :type/nature :user]
    [?e :field/_parent ?f]
    [?f [:field/name] ?id]])

(defn q-test [query]
  (d/q query @meta-db))


(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")


(when (io/resource "local.clj")
  (load "local"))


(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
