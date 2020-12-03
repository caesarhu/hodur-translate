(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [datoteka.core :as fs]
    [hodur-translate.postgres-sql :as sql]
    [hodur-translate.utils :as utils]
    [camel-snake-kebab.core :as csk]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parsing functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn field-type-name
  [field]
  (-> field :field/type :type/name))


(defmulti  primitive-or-ref-type
  (fn [field]
    (field-type-name field)))


(defmethod primitive-or-ref-type "String" [_] {:postgres/type :postgres.type/text})

(defmethod primitive-or-ref-type "Float" [_] {:postgres/type :postgres.type/decimal})

(defmethod primitive-or-ref-type "Integer" [_] {:postgres/type :postgres.type/bigint})

(defmethod primitive-or-ref-type "Boolean" [_] {:postgres/type :postgres.type/boolean})

(defmethod primitive-or-ref-type "Date" [_] {:postgres/type :postgres.type/date})

(defmethod primitive-or-ref-type "DateTime" [_] {:postgres/type :postgres.type/timestamp})

(defmethod primitive-or-ref-type "ID" [_] {:postgres/type :postgres.type/uuid})


(defmethod primitive-or-ref-type :default [field]
  (let [type-name (field-type-name field)
        is-enum? (-> field :field/type :type/enum)]
    (if is-enum?
      {:postgres/type (keyword "postgres.type" (csk/->kebab-case-string type-name))}
      {:postgres/type              (-> field :field/type :ref-type :postgres/type)
       :postgres.column/references (csk/->snake_case_keyword type-name)})))


(defn get-value-type
  [field]
  (if-let [dat-type (-> field :postgres/type)]
    dat-type
    (primitive-or-ref-type field)))


(defn get-identity
  [type]
  (let [fields (get type :field/_parent)
        primary-field (sp/select-one [sp/ALL #(:postgres.column/primary-key %)] fields)]
    (when primary-field
      (get-value-type primary-field))))


(defn get-type-identity
  [types type-name]
  (let [type (sp/select-one [sp/ALL #(= type-name (:type/name %))] types)
        is-enum? (get type :type/enum)]
    (when-not is-enum?
      (get-identity type))))


(defn is-ref-type?
  [field]
  (and (-> field field-type-name utils/primary-type? not)
       (-> field :field/type :type/enum not)))


(defn merge-ref-type
  [types]
  (sp/transform [sp/ALL :field/_parent sp/ALL #(is-ref-type? %)]
                (fn [field]
                  (let [ref-type (get-type-identity types (field-type-name field))]
                    (assoc-in field [:field/type :ref-type] ref-type)))
                types))


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
  {:field/default :postgres.column/default
   :field/optional  :postgres.column/optional})


(defn assoc-attributes
  [m field]
  (reduce-kv (fn [a k v]
               (if-let [entry (get postgres-translate-table k)]
                 (assoc a entry v)
                 a))
             m field))


(defn get-postgres-attr
  [field]
  (let [postgres? (fn [entry]
                    (re-find #"postgres" (namespace (key entry))))]
    (->> (sp/select [sp/ALL #(postgres? %)] field)
         (into {}))))


(defn process-field
  [entity-id is-enum? {:keys [field/name] :as field}]
  (let [postgres-field (cond-> {:postgres/ident (keyword entity-id
                                                         (csk/->kebab-case-string name))}
                               ;(not is-enum?) (assoc :postgres/cardinality (get-cardinality field))
                         (not is-enum?) (merge (get-value-type field))
                         (not is-enum?) (assoc-attributes field)

                         :always        (assoc-documentation field))]
    (merge postgres-field (get-postgres-attr field))))


(defn get-type
  [{:keys [type/name type/enum field/_parent]}]
  (let [entity-id (csk/->kebab-case-string name)]
    (->> _parent
         (sort-by :field/name)
         (reduce (fn [c {:keys [postgres/tag] :as field}]
                   (if tag
                     (conj c (process-field entity-id enum field))
                     c))
                 []))))


(defn type->table
  [type]
  (let [table (dissoc type :field/_parent)
        column (get-type type)]
    (assoc table :column column)))


(defn types->tables
  [types]
  (->> (map type->table types)
       (sort-by :db/id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-schema
  [conn]
  (let [types (->> (utils/get-id-types conn :postgres/tag)
                   merge-ref-type)
        result (types->tables types)]
    result))


(defn schema->sql
  [schema-v]
  (map sql/make-schema-sql (sql/set-table-order schema-v)))


(defn schema
  [conn]
  (let [raw (db-schema conn)]
    (schema->sql raw)))


(defn save-schema-sql
  [schema-v path]
  (when (string? path)
    (let [new-path (string/replace (str path "/") #"//" "/")]
      (fs/create-dir new-path)
      (dorun (for [schema schema-v]
               (sql/save-sql schema new-path))))))


(defn save-db-sql
  [conn path]
  (save-schema-sql (schema conn) path))


