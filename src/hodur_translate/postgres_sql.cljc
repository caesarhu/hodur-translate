(ns hodur-translate.postgres-sql
  (:require
    [clojure.spec.alpha :as s]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [datoteka.core :as fs]
    [hodur-translate.postgres-format :as pf]
    [hodur-translate.utils :as utils]
    [honeysql-postgres.helpers :as psqlh]
    [honeysql.core :as sql]
    [honeysql.format :as sqlf]
    [honeysql.helpers :as sqlh])
  (:import (com.github.vertical_blank.sqlformatter SqlFormatter)))

(def sql-cmd-end-str ";")
(def string-quote "'")
(def statement-separator "\n--;;\n")

;;wrap strings in quotes
(defn sql-param
  [param]
  (if (string? param)
    (str string-quote param string-quote)
    (str param)))

(s/fdef sql-param
  :args (s/cat :param string?)
  :ret string?)

;;recursively get rid of those pesky "?" by replacing them
;;with the parameter list
(defn replace-params
  [sql-str params]
  (if (empty? params)
    sql-str
    (let [param (first params)
          new-str (string/replace-first sql-str #"\?" (sql-param param))]
      (recur new-str (rest params)))))

(s/fdef replace-params
  :args (s/cat :sql-str string? :params (s/nilable seq?))
  :ret string?)

(defn ->command
  [sql-v]
  (replace-params (first sql-v) (rest sql-v)))

(defn sql-command
  [m]
  (-> m sql/format ->command))

(s/fdef sql-command
  :args (s/cat :m (s/map-of keyword? any?))
  :ret string?)


(defn get-schema-table-name
  [postgres-schema]
  (-> postgres-schema :type/kebab-case-name))


(defn column-format
  [column]
  (let [useless [:postgres/ident :postgres/type :postgres/tag]
        {:keys [postgres/ident postgres/type postgres.column/optional]} column
        params (->> (apply dissoc column useless)
                    (transform-keys csk/->kebab-case-keyword)
                    (merge {:optional optional}))]
    (concat [(-> ident csk/->kebab-case-keyword)
             (-> type csk/->kebab-case-keyword)]
            (map #(apply sql/call %) params))))

(defn table-format
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        columns (:column postgres-schema)
        base (psqlh/create-table {} table)]
    (psqlh/with-columns base (map column-format columns))))

(defn index-format
  [params]
  (-> (pf/create-index params)
      sql-command))

(defn create-index-sql
  [postgres-schema]
  (when-let [index (:postgres.table/index postgres-schema)]
    (let [indexes (if (coll? (first index))
                    index
                    [index])]
      (map index-format indexes))))

(defn create-table-sql
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)]
    {:up-sql (concat [(-> postgres-schema table-format sql-command)]
                     (create-index-sql postgres-schema))
     :down-sql [(->> (psqlh/drop-table table)
                     sql-command)]}))


(defn create-type-sql
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        columns (->> (:column postgres-schema)
                     (map :postgres/ident)
                     (map name))]
    {:up-sql [(->> (pf/create-enum table columns)
                   sql-command)]
     :down-sql [(->> (pf/drop-enum table)
                     sql-command)]}))


(defn create-up-sql
  [postgres-schema]
  (cond
    (:type/enum postgres-schema) (create-type-sql postgres-schema)
    :else (create-table-sql postgres-schema)))

(defn sql-style
  [s]
  (.. SqlFormatter (format s)))

(defn make-sql-str
  [sql-v]
  (->> (map sql-style sql-v)
       (map #(str % sql-cmd-end-str))
       (string/join statement-separator)))


(defn number-header
  [n]
  (format "%03d" n))


(def create-file-header "create")
(def up-footer ".up.sql")
(def down-footer ".down.sql")


(defn make-ragtime-filename
  [postgres-schema]
  (let [table (-> (get-schema-table-name postgres-schema) name)
        order (:postgres.table/table-order postgres-schema)
        base-name (string/join "-" [(number-header order) create-file-header table])]
    {:up-name (csk/->kebab-case-string (str base-name up-footer))
     :down-name (csk/->kebab-case-string (str base-name down-footer))}))


(defn make-schema-sql
  [postgres-schema]
  (merge (create-up-sql postgres-schema)
         (make-ragtime-filename postgres-schema)))


(defn save-sql
  [sql-schema path]
  (let [{:keys [up-name down-name up-sql down-sql]} sql-schema]
    (spit (str path up-name) (make-sql-str up-sql))
    (spit (str path down-name) (make-sql-str down-sql))))


(defn create-order
  [schema-v]
  (let [order-exist (->> (map :postgres.table/table-order schema-v)
                         (filter some?)
                         set)
        order-v (->> (range 1 (inc (count schema-v)))
                     (filter #(not (contains? order-exist %))))]
    order-v))


(defn set-table-order
  [schema-v]
  (let [new-order (create-order schema-v)
        schema-map (group-by #(nil? (:postgres.table/table-order %)) schema-v)
        unorder-schema (get schema-map true)
        ordered-schema (get schema-map false)]
    (-> (map #(assoc %1 :postgres.table/table-order %2) unorder-schema new-order)
        (concat ordered-schema))))
