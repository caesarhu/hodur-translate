(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
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

(def postgres-sql-column-table
  {:postgres.constraint/optional    (fn [s v]
                                      (if v
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
                                      (if v
                                        (let [table (-> v namespace ->snake_case_string)
                                              column (-> v name ->snake_case_string)]
                                          (str s " REFERENCES " table " (" column ")")))
                                      s)
   :postgres/ref-update             (fn [s v]
                                      (if (keyword? v)
                                        (str s " ON UPDATE " (-> v name ->SCREAMING_SNAKE_CASE_STRING))
                                        s))
   :postgres/ref-delete             (fn [s v]
                                      (if (keyword? v)
                                        (str s " ON DELETE " (-> v name ->SCREAMING_SNAKE_CASE_STRING))
                                        s))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn schema
  [conn]
  (let [types (utils/get-eids-types conn :postgres/tag)]
    (->> types
         (reduce (fn [c t]
                   (concat c (postgres-get-type types t)))
                 [])
         vec)))


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
      (schema conn))))


