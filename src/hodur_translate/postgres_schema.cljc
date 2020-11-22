(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [datoteka.core :as fs]
    [hodur-translate.postgres-sql :as sql]
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

(defmethod primitive-or-ref-type "Date" [_] :postgres.type/date)

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


(defn get-type-identity
  [types type-name]
  (let [type (sp/select-one [sp/ALL #(= type-name (:type/name %))] types)
        is-enum? (get type :type/enum)]
    (cond
      is-enum? {:db/valueType (keyword (namespace :postgres.type/ref) type-name)
                :postgres/enum (keyword type-name)}
      :else (get-identity type))))


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
  [types entity-id is-enum? {:keys [field/name] :as field}]
  (let [ref-type (-> field :field/type :type/name)]
    (cond-> {:db/ident (keyword entity-id
                                (->kebab-case-string name))}
      (not is-enum?) (assoc :db/valueType (get-value-type field)
                            :db/cardinality (get-cardinality field))
      (not is-enum?) (assoc-attributes field)
      (= :postgres.type/ref (get-value-type field)) (merge (get-type-identity types ref-type))

      :always        (assoc-documentation field))))


(defn postgres-get-type
  [types {:keys [type/name type/enum field/_parent]}]
  (let [entity-id (->kebab-case-string name)]
    (->> _parent
         (sort-by :field/name)
         (reduce (fn [c {:keys [postgres/tag] :as field}]
                   (if tag
                     (conj c (postgres-process-field types entity-id enum field))
                     c))
                 []))))


(defn type->table
  [types type]
  (let [table (dissoc type :field/_parent)
        column (postgres-get-type types type)]
    (assoc table :column column)))


(defn types->tables
  [types]
  (->> (map (partial type->table types) types)
       (sort-by :db/id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-schema
  [conn]
  (let [types (utils/get-eids-types conn :postgres/tag)
        result (types->tables types)]
    result))


(defn schema->sql
  [schema-v]
  (map sql/make-schema-sql schema-v))


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


