(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [datoteka.core :as fs]
    [hodur-translate.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parsing functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti  primitive-or-ref-type
  (fn [field]
    (let [ref-type (-> field :field/type :type/name)
          dat-type (-> field :postgres/type)]
      (or dat-type ref-type))))


(defmethod primitive-or-ref-type "String" [_] :postgres.type/text)

(defmethod primitive-or-ref-type "Float" [_] :postgres.type/decimal)

(defmethod primitive-or-ref-type "Integer" [_] :postgres.type/bigint)

(defmethod primitive-or-ref-type "Boolean" [_] :postgres.type/boolean)

(defmethod primitive-or-ref-type "DateTime" [_] :postgres.type/timestamp)

(defmethod primitive-or-ref-type "ID" [_] :postgres.type/uuid)

(defmethod primitive-or-ref-type :default [_] :postgres.type/ref)


(defn get-value-type
  [field]
  (if-let [dat-type (-> field :postgres/type)]
    dat-type
    (primitive-or-ref-type field)))


(defn get-identity
  [type]
  (let [fields (get type :field/_parent)
        primary-field (sp/select-one [sp/ALL #(:postgres/primary-key %)] fields)]
    (when primary-field
      {:postgres/ref (keyword (name (:type/name type)) (:field/name primary-field))
       :db/valueType (get-value-type primary-field)})))


(def ^:private schema-types (atom nil))


(defn get-type-identity
  [type-name]
  (some-> (sp/select-one [sp/ALL #(= type-name (:type/name %))] @schema-types)
          get-identity))


(defn get-cardinality
  [{:keys [field/cardinality]}]
  (if cardinality
    (if (and (= (first cardinality) 1)
             (= (second cardinality) 1))
      :db.cardinality/one
      :db.cardinality/many)
    :db.cardinality/one))


(defn assoc-documentation
  [m {:keys [field/doc field/deprecation]}]
  (if (or doc deprecation)
    (assoc m :db/doc
           (cond-> ""
             doc                   (str doc)
             (and doc deprecation) (str "\n\n")
             deprecation           (str "DEPRECATION NOTE: " deprecation)))
    m))


(def postgres-translate-table
  {:postgres/index  :postgres/index
   :postgres/unique :postgres.constraint/unique
   :postgres/primary-key :postgres.constraint/primary-key
   :postgres/auto-increment :postgres/auto-increment
   :postgres/ref :postgres/ref
   :postgres/ref-update :postgres/ref-update
   :postgres/ref-delete :postgres/ref-delete
   :field/optional  :postgres.constraint/optional})


(defn assoc-attributes
  [m field]
  (reduce-kv (fn [a k v]
               (if-let [entry (get postgres-translate-table k)]
                 (assoc a entry v)
                 a))
             m field))


(defn postgres-process-field
  [entity-id is-enum? {:keys [field/name] :as field}]
  (let [ref-type (-> field :field/type :type/name)]
    (cond-> {:db/ident (keyword entity-id
                                (->kebab-case-string name))}
      (not is-enum?) (assoc :db/valueType (get-value-type field)
                            :db/cardinality (get-cardinality field))
      (not is-enum?) (assoc-attributes field)
      (= :postgres.type/ref (get-value-type field)) (merge (get-type-identity ref-type))

      :always        (assoc-documentation field))))


(defn postgres-get-type
  [{:keys [type/name type/enum field/_parent]}]
  (let [entity-id (->kebab-case-string name)]
    (->> _parent
         (sort-by :field/name)
         (reduce (fn [c {:keys [postgres/tag] :as field}]
                   (if tag
                     (conj c (postgres-process-field entity-id enum field))
                     c))
                 []))))


(defn get-sql-table-snake
  [schema]
  (-> schema :db/ident namespace ->snake_case_string))


(defn get-sql-column
  [schema]
  (-> schema :db/ident name ->snake_case_string))


(defn get-sql-column-type
  [schema]
  (-> schema :db/valueType name ->SCREAMING_SNAKE_CASE_STRING))

(defn create-sql-column
  [schema]
  (cond-> (str (get-sql-column schema) " " (get-sql-column-type schema))
    (not (:postgres.constraint/optional schema)) (str " NOT NULL")
    (:postgres/auto-increment schema) (str " GENERATED ALWAYS AS IDENTITY")
    (:postgres.constraint/unique schema) (str " UNIQUE")
    (:postgres.constraint/primary-key schema) (str " PRIMARY KEY")
    (keyword? (:postgres/ref schema)) (str " REFERENCES " (namespace (:postgres/ref schema))
                                           " (" (name (:postgres/ref schema)) ")")
    (keyword? (:postgres/ref-update schema)) (str " ON UPDATE " (-> (:postgres/ref-update schema)
                                                                    name
                                                                    ->SCREAMING_SNAKE_CASE_STRING))
    (keyword? (:postgres/ref-delete schema)) (str " ON UPDATE " (-> (:postgres/ref-delete schema)
                                                                    name
                                                                    ->SCREAMING_SNAKE_CASE_STRING))))


(defn make-column-index
  [schema]
  (when (:postgres/index schema)
    (let [table (get-sql-table-snake schema)
          column (get-sql-column schema)]
      [(symbol "--;;")
       (->> (str "CREATE INDEX " table "_" column "_index ON " table " (" column ") ;")
            symbol)])))


(defn make-sql-table
  [postgres-schema table-key]
  (let [table (-> table-key name ->snake_case_string)
        columns (get-in postgres-schema [table-key :column])
        create-header (str "CREATE TABLE " table)
        drop-header (str "DROP TABLE " table " ;")
        column-def (let [str-v (map create-sql-column columns)
                         header (-> (map #(str % " ,") (drop-last str-v))
                                    vec)]
                     (apply list (conj header (last str-v))))
        index-def (->> (map make-column-index columns)
                       (filter some?)
                       (apply concat))]
    {:create-table (sp/transform [(sp/walker string?)]
                                 symbol
                                 (concat [create-header column-def ";"] index-def))
     :drop-table [(symbol drop-header)]}))


(defn make-sql-str
  ([sql-v opts]
   (->> (map #(utils/pretty-str % opts) sql-v)
        (string/join)))
  ([sql-v]
   (make-sql-str sql-v nil)))


(defn number-header
  [n]
  (format "%03d" n))


(def create-header "create")
(def up-footer ".up.sql")
(def down-footer ".down.sql")


(defn make-ragtime-filename
  [postgres-schema table-key]
  (let [table (name table-key)
        order (get-in postgres-schema [table-key :postgres/table-order])
        base-name (string/join "-" [(number-header order) create-header table])]
    {:up-name (str base-name up-footer)
     :down-name (str base-name down-footer)}))


(defn postgres-or-name?
  [entry]
  (let [ek (key entry)]
    (or (= ek :type/name)
        (= "postgres" (namespace ek)))))


(defn type->table
  [type]
  (let [table (->> (filter postgres-or-name? type)
                   (into {}))
        table-key (-> table :type/name ->kebab-case-string keyword)]
    {table-key (assoc table :column (postgres-get-type type))}))


(defn types->tables
  [types]
  (->> (map type->table types)
       (apply merge)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-schema
  [conn]
  (let [types (utils/get-eids-types conn :postgres/tag)
        _ (reset! schema-types types)
        result (types->tables types)]
    (reset! schema-types nil)
    result))


(defn schema->sql
  [schema]
  (let [table-keys (keys schema)]
    (reduce (fn [res k]
              (let [m (make-sql-table schema k)
                    fm (merge m (make-ragtime-filename schema k))]
                (update res k merge fm)))
            schema
            table-keys)))


(defn schema
  [conn]
  (let [raw (db-schema conn)]
    (schema->sql raw)))


(defn save-schema-sql
  [schema path]
  (when (string? path)
    (let [new-path (string/replace (str path "/") #"//" "/")]
      (fs/create-dir new-path)
      (dorun (for [table-key (keys schema)
                   :let [{:keys [up-name down-name create-table drop-table]} (get schema table-key)
                         up-file (str new-path up-name)
                         down-file (str new-path down-name)]]
               (do
                 (spit up-file (make-sql-str create-table))
                 (spit down-file (make-sql-str drop-table))))))))


(defn save-db-sql
  [conn path]
  (save-schema-sql (schema conn) path))


