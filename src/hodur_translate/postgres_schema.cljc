(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [hodur-translate.utils :as utils]
    [datoteka.core :as fs]))

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


(defn  get-value-type
  [field]
  (if-let [dat-type (-> field :postgres/type)]
    dat-type
    (primitive-or-ref-type field)))

(defn get-identity [type]
  (let [fields (get type :field/_parent)
        primary-field (sp/select-one [sp/ALL #(:postgres/primary-key %)] fields)]
    (when primary-field
      {:postgres/ref (keyword (name (:type/name type)) (:field/name primary-field))
       :db/valueType (get-value-type primary-field)})))

(defn get-type-identity [types type-name]
  (some-> (sp/select-one [sp/ALL #(= type-name (:type/name %))] types)
          get-identity))


(defn  get-cardinality
  [{:keys [field/cardinality]}]
  (if cardinality
    (if (and (= (first cardinality) 1)
             (= (second cardinality) 1))
      :db.cardinality/one
      :db.cardinality/many)
    :db.cardinality/one))


(defn  assoc-documentation
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

(defn  assoc-attributes
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

(def postgres-sql-column-options
  {:postgres.constraint/optional    (fn [s v]
                                      (if-not v
                                        (str s " NOT NULL")
                                        s))
   :postgres/auto-increment         (fn [s v]
                                      (if v
                                        (str s " GENERATED ALWAYS AS IDENTITY")
                                        s))
   :postgres.constraint/unique      (fn [s v]
                                      (if v
                                        (str s " UNIQUE")
                                        s))
   :postgres.constraint/primary-key (fn [s v]
                                      (if v
                                        (str s " PRIMARY KEY")
                                        s))
   :postgres/ref                    (fn [s v]
                                      (if (keyword? v)
                                        (str s " REFERENCES " (namespace v) " (" (name v) ")")
                                        s))
   :postgres/ref-update             (fn [s v]
                                      (if (keyword? v)
                                        (str s " ON UPDATE " (-> v name ->SCREAMING_SNAKE_CASE_STRING))
                                        s))
   :postgres/ref-delete             (fn [s v]
                                      (if (keyword? v)
                                        (str s " ON DELETE " (-> v name ->SCREAMING_SNAKE_CASE_STRING))
                                        s))})

(defn get-sql-table [schema]
  (-> schema :db/ident namespace keyword))

(defn get-sql-table-snake [schema]
  (-> schema :db/ident namespace ->snake_case_string))

(defn get-sql-column [schema]
  (-> schema :db/ident name ->snake_case_string))

(defn get-sql-column-type [schema]
  (-> schema :db/valueType name ->SCREAMING_SNAKE_CASE_STRING))

(defn group-schema [schema]
  (let [table-keys (->> (map get-sql-table schema)
                        set)
        table-map (zipmap table-keys (repeat []))]
    (reduce (fn [res m]
              (let [table (get-sql-table m)]
                (update res table conj m)))
            table-map
            schema)))

(def sql-column-options
  [:postgres.constraint/optional :postgres/auto-increment :postgres.constraint/unique
   :postgres.constraint/primary-key :postgres/ref :postgres/ref-update :postgres/ref-delete])

(defn make-sql-column [schema]
  (let [column-origin (str (get-sql-column schema) " " (get-sql-column-type schema))]
    (reduce (fn [res k]
              (let [f (get postgres-sql-column-options k)
                    val (get schema k)]
                (f res val)))
            column-origin
            sql-column-options)))

(defn make-column-index [schema]
  (when (:postgres/index schema)
    (let [table (get-sql-table-snake schema)
          column (get-sql-column schema)]
      [(symbol "--;;")
       (->> (str "CREATE INDEX " table "_" column "_index ON " table " (" column ") ;")
            symbol)])))

(defn make-sql-table [postgres-schema table-key]
  (let [table (-> table-key name ->snake_case_string)
        columns (get-in postgres-schema [table-key :column])
        create-header (str "CREATE TABLE " table)
        drop-header (str "DROP TABLE " table " ;")
        column-def (let [str-v (map make-sql-column columns)
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

(defn number-header [n]
  (format "%03d" n))

(def create-header "create")
(def up-footer ".up.sql")
(def down-footer ".down.sql")

(defn make-ragtime-filename [postgres-schema table-key]
  (let [table (name table-key)
        order (get-in postgres-schema [table-key :table-order])
        base-name (string/join "-" [(number-header order) create-header table])]
    {:up-name (str base-name up-footer)
     :down-name (str base-name down-footer)}))

(defn get-table-orders [types]
  (let [orders (sp/select [sp/ALL #(number? (:postgres/table-order %)) (sp/submap [:type/name :postgres/table-order])]
                          types)]
    (reduce (fn [res m]
              (let [{:keys [type/name postgres/table-order]} m]
                (assoc res (-> name ->kebab-case-string keyword) {:table-order table-order})))
            {}
            orders)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn raw-schema
  [conn]
  (let [types (utils/get-eids-types conn :postgres/tag)
        tables (get-table-orders types)
        raw-columns (->> types
                         (reduce (fn [c t]
                                   (concat c (postgres-get-type types t)))
                                 [])
                         vec)
        group-columns (group-schema raw-columns)]
    (reduce (fn [res k]
              (let [column (get group-columns k)]
                (update res k assoc :column column)))
            tables
            (keys group-columns))))

(defn schema->sql [schema]
  (let [table-keys (keys schema)
        middle-schema (reduce (fn [res k]
                                (let [m (make-sql-table schema k)]
                                  (update res k merge m)))
                              schema
                              table-keys)]
    (reduce (fn [res k]
              (let [m (make-ragtime-filename middle-schema k)]
                (update res k merge m)))
            middle-schema
            table-keys)))

(defn schema [conn]
  (let [raw (raw-schema conn)]
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


(comment
  (do
    (require '[hodur-engine.core :as engine])

    (def conn (engine/init-schema
                '[^{:postgres/tag true}
                  default

                  ^:interface
                  Person
                  [^String name]

                  Employee
                  [^String name
                   ^{:type String
                     :doc "The very employee number of this employee"
                     :postgres/unique :db.unique/identity}
                   number
                   ^Float salary
                   ^Integer age
                   ^DateTime start-date
                   ^Employee supervisor
                   ^{:type Employee
                     :cardinality [0 n]
                     :doc "Has documentation"
                     :deprecation "But also deprecation"}
                   co-workers
                   ^{:postgres/type :postgres.type/keyword}
                   keyword-type
                   ^{:postgres/type :postgres.type/uri}
                   uri-type
                   ^{:postgres/type :postgres.type/double}
                   double-type
                   ^{:postgres/type :postgres.type/bigdec
                     :deprecation "This is deprecated"}
                   bigdec-type
                   ^EmploymentType employment-type
                   ^SearchResult last-search-results]

                  ^{:union true}
                  SearchResult
                  [Employee Person EmploymentType]

                  ^{:enum true}
                  EmploymentType
                  [FULL_TIME
                   ^{:doc "Documented enum"}
                   PART_TIME]]))

    (clojure.pprint/pprint
      (raw-schema conn))))


