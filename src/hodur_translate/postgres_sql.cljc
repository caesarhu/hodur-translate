(ns hodur-translate.postgres-sql
  (:require
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
    [honeysql.helpers :as sqlh]))


(defn sql-format
  [m]
  (-> m sql/format first (str ";")))


(def sql-cmd-seperater "--;;")
(def sql-cmd-end-str " ;")
(def sql-column-seperater " ,")
(def create-table-header  "CREATE TABLE ")
(def create-index-header  "CREATE INDEX ")
(def create-type-header  "CREATE TYPE ")
(def drop-table-header "DROP TABLE ")
(def drop-type-header "DROP TYPE ")


(defn get-sql-table-snake
  [schema]
  (-> schema :db/ident namespace csk/->snake_case_string))


(defn get-sql-column
  [schema]
  (-> schema :db/ident name csk/->snake_case_string))


(defn get-sql-column-type
  [schema]
  (let [type (-> schema :db/valueType name)]
    (if (:postgres/enum schema)
      (csk/->snake_case_string type)
      (csk/->SCREAMING_SNAKE_CASE_STRING type))))


(defn parentheses
  [s]
  (str "( " s " )"))


(defn quotation
  [s]
  (str "'" s "'"))


(defn SNAKE_CASE_NAME
  [k]
  (-> k name csk/->SCREAMING_SNAKE_CASE_STRING))


(defn str-or-key?
  [s]
  (or (string? s)
      (keyword? s)))


(defn create-column-sql
  [schema]
  (cond-> (str (get-sql-column schema) " " (get-sql-column-type schema))
    (not (:postgres.constraint/optional schema)) (str " NOT NULL")
    (:field/default schema) (str " DEFAULT " (:field/default schema))
    (str-or-key? (:postgres/auto-increment schema)) (str " GENERATED " (-> (:postgres/auto-increment schema)
                                                                           name
                                                                           string/upper-case) " AS IDENTITY")
    (:postgres.constraint/unique schema) (str " UNIQUE")
    (:postgres.constraint/primary-key schema) (str " PRIMARY KEY")
    (:postgres/ref schema) (str " REFERENCES " (namespace (:postgres/ref schema))
                                (parentheses (-> schema :postgres/ref name csk/->snake_case_string)))
    (str-or-key? (:postgres/ref-update schema)) (str " ON UPDATE " (-> (:postgres/ref-update schema)
                                                                       SNAKE_CASE_NAME))
    (str-or-key? (:postgres/ref-delete schema)) (str " ON DELETE " (-> (:postgres/ref-delete schema)
                                                                       SNAKE_CASE_NAME))))


(defn create-column-index-sql
  [schema]
  (when (:postgres/index schema)
    (let [table (get-sql-table-snake schema)
          column (get-sql-column schema)]
      [(symbol sql-cmd-seperater)
       (->> (str create-index-header table "_" column "_index ON " table " " (parentheses column) sql-cmd-end-str)
            symbol)])))


(defn get-schema-table-name
  [postgres-schema]
  (-> postgres-schema :type/kebab-case-name))


(defn create-table-sql
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        columns (:column postgres-schema)
        create-header (str create-table-header table)
        column-def (let [str-v (map create-column-sql columns)
                         header (-> (map #(str % " ,") (drop-last str-v))
                                    vec)]
                     (apply list (conj header (last str-v))))
        index-def (->> (map create-column-index-sql columns)
                       (filter some?)
                       (apply concat))]
    {:up-sql (sp/transform [(sp/walker string?)]
                           symbol
                           (concat [create-header column-def ";"] index-def))
     :down-sql [(symbol (str drop-table-header table sql-cmd-end-str))]}))


(defn column-format
  [column]
  (let [useless [:postgres/ident :postgres/type :postgres/tag]
        {:keys [postgres/ident postgres/type]} column
        params (->> (apply dissoc column useless)
                    (transform-keys csk/->kebab-case-keyword))]
    (-> (concat [(-> ident csk/->kebab-case-keyword)
                 (-> type csk/->kebab-case-keyword)]
                (map #(apply sql/call %) params))
        vec)))

(defn table-format
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        columns (:column postgres-schema)]
    (-> (psqlh/create-table table)
        (psqlh/with-columns [(map column-format columns)]))))


(defn create-type-sql
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        columns (->> (:column postgres-schema)
                     (map :postgres/ident)
                     (map name))]
    {:up-sql [(->> (pf/create-enum table columns)
                   sql-format)]
     :down-sql [(->> (pf/drop-enum table)
                     sql-format)]}))


(defn create-up-sql
  [postgres-schema]
  (cond
    (:type/enum postgres-schema) (create-type-sql postgres-schema)
    :else (create-table-sql postgres-schema)))


(defn make-sql-str
  ([sql-v opts]
   (->> (map #(utils/pretty-str % opts) sql-v)
        (string/join)))
  ([sql-v]
   (make-sql-str sql-v nil)))


(defn number-header
  [n]
  (format "%03d" n))


(def create-file-header "create")
(def up-footer ".up.sql")
(def down-footer ".down.sql")


(defn make-ragtime-filename
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        order (:postgres/table-order postgres-schema)
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
  (let [order-exist (->> (map :postgres/table-order schema-v)
                         (filter some?)
                         set)
        order-v (->> (range 1 (inc (count schema-v)))
                     (filter #(not (contains? order-exist %))))]
    order-v))


(defn set-table-order
  [schema-v]
  (let [new-order (create-order schema-v)
        schema-map (group-by #(nil? (:postgres/table-order %)) schema-v)
        unorder-schema (get schema-map true)
        ordered-schema (get schema-map false)]
    (-> (map #(assoc %1 :postgres/table-order %2) unorder-schema new-order)
        (concat ordered-schema))))
