(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string]]
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


(defmethod primitive-or-ref-type "String" [_] :db.type/string)

(defmethod primitive-or-ref-type "Float" [_] :db.type/float)

(defmethod primitive-or-ref-type "Integer" [_] :db.type/long)

(defmethod primitive-or-ref-type "Boolean" [_] :db.type/boolean)

(defmethod primitive-or-ref-type "DateTime" [_] :db.type/date-time)

(defmethod primitive-or-ref-type "ID" [_] :db.type/uuid)

(defmethod primitive-or-ref-type :default [_] :db.type/ref)


(defn  get-value-type
  [field]
  (if-let [dat-type (-> field :postgres/type)]
    dat-type
    (primitive-or-ref-type field)))

(defn get-identity [type]
  (let [fields (get type :field/_parent)]
    (some #(and (= :db.unique/identity (:postgres/unique %))
                {:postgres/ref (keyword (name (:type/name type)) (:field/name %))
                 :db/valueType (get-value-type %)})
          fields)))

(defn get-type-identity [types type-name]
  (let [type (sp/select-one [sp/ALL #(= type-name (:type/name %))] types)
        fields (get type :field/_parent)]
    (some #(and (= :db.unique/identity (:postgres/unique %))
                {:postgres/ref (keyword (name type-name) (:field/name %))
                 :db/valueType (get-value-type %)})
          fields)))


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


(defn  assoc-attributes
  [m field]
  (let [table {:postgres/isComponent :db/isComponent
               :postgres/fulltext :db/fulltext
               :postgres/index :db/index
               :postgres/unique :db/unique
               :postgres/noHistory :db/noHistory
               :field/optional :postgres/optional}]
    (reduce-kv (fn [a k v]
                 (if-let [entry (get table k)]
                   (assoc a entry v)
                   a))
               m field)))


(defn postgres-process-field
  [types entity-id is-enum? {:keys [field/name] :as field}]
  (let [ref-type (-> field :field/type :type/name)]
    (cond-> {:db/ident (keyword entity-id
                                (->kebab-case-string name))}
      (not is-enum?) (assoc :db/valueType (get-value-type field)
                            :db/cardinality (get-cardinality field))
      (not is-enum?) (assoc-attributes field)
      (= :db.type/ref (get-value-type field)) (merge (get-type-identity types ref-type))

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
                   ^{:postgres/type :db.type/keyword}
                   keyword-type
                   ^{:postgres/type :db.type/uri}
                   uri-type
                   ^{:postgres/type :db.type/double}
                   double-type
                   ^{:postgres/type :db.type/bigdec
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


