(ns hodur-translate.postgres-sql
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [datoteka.core :as fs]
    [hodur-translate.utils :as utils]))


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
  (-> schema :db/ident namespace ->snake_case_string))


(defn get-sql-column
  [schema]
  (-> schema :db/ident name ->snake_case_string))


(defn get-sql-column-type
  [schema]
  (let [type (-> schema :db/valueType name)]
    (if (:postgres/enum schema)
      (->snake_case_string type)
      (->SCREAMING_SNAKE_CASE_STRING type))))


(defn parentheses
  [s]
  (str "( " s " )"))


(defn quotation
  [s]
  (str "'" s "'"))


(defn create-column-sql
  [schema]
  (cond-> (str (get-sql-column schema) " " (get-sql-column-type schema))
    (not (:postgres.constraint/optional schema)) (str " NOT NULL")
    (:postgres/auto-increment schema) (str " GENERATED ALWAYS AS IDENTITY")
    (:postgres.constraint/unique schema) (str " UNIQUE")
    (:postgres.constraint/primary-key schema) (str " PRIMARY KEY")
    (:postgres/ref schema) (str " REFERENCES " (namespace (:postgres/ref schema))
                                (parentheses (name (:postgres/ref schema))))
    (:postgres/ref-update schema) (str " ON UPDATE " (-> (:postgres/ref-update schema)
                                                         name
                                                         ->SCREAMING_SNAKE_CASE_STRING))
    (:postgres/ref-update schema) (str " ON UPDATE " (-> (:postgres/ref-delete schema)
                                                         name
                                                         ->SCREAMING_SNAKE_CASE_STRING))))


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
  (-> postgres-schema :type/snake_case_name name))


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


(defn create-type-sql
  [postgres-schema]
  (let [table (get-schema-table-name postgres-schema)
        columns (->> (:column postgres-schema)
                     (map :db/ident)
                     (map name)
                     (map quotation))]
    {:up-sql [(-> (str create-type-header table " AS ENUM "
                       (parentheses (string/join " " columns)) sql-cmd-end-str)
                  symbol)]
     :down-sql [(-> (str drop-type-header table sql-cmd-end-str)
                    symbol)]}))


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
    {:up-name (->kebab-case-string (str base-name up-footer))
     :down-name (->kebab-case-string (str base-name down-footer))}))


(defn make-schema-sql
  [postgres-schema]
  (merge (create-up-sql postgres-schema)
         (make-ragtime-filename postgres-schema)))


(defn save-sql
  [sql-schema path]
  (let [{:keys [up-name down-name up-sql down-sql]} sql-schema]
    (spit (str path up-name) (make-sql-str up-sql))
    (spit (str path down-name) (make-sql-str down-sql))))
