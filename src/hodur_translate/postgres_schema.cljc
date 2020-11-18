(ns hodur-translate.postgres-schema
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string]]
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select transform setval]])))

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

(defn get-type-identity [conn type-name]
  (let [selector '[* {:field/_parent
                      [* {:field/type [*]}]}]
        eids (-> (d/q '[:find [?e ...]
                        :in $ ?type-name
                        :where
                        [?e :type/name ?type-name]]
                      @conn (name type-name))
                 vec flatten)
        type (->> eids
                  (d/pull-many @conn selector)
                  (sort-by :type/name)
                  first)
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
               :postgres/noHistory :db/noHistory}]
    (reduce-kv (fn [a k v]
                 (if-let [entry (get table k)]
                   (assoc a entry v)
                   a))
               m field)))


(defn  process-field
  [entity-id is-enum? {:keys [field/name] :as field}]
  (cond-> {:db/ident (keyword entity-id
                              (->kebab-case-string name))}
          (not is-enum?) (assoc :db/valueType (get-value-type field)
                                :db/cardinality (get-cardinality field))
          (not is-enum?) (assoc-attributes field)

          :always        (assoc-documentation field)))


(defn  get-type
  [{:keys [type/name type/enum field/_parent]}]
  (let [entity-id (->kebab-case-string name)]
    (->> _parent
         (sort-by :field/name)
         (reduce (fn [c {:keys [postgres/tag] :as field}]
                   (if tag
                     (conj c (process-field entity-id enum field))
                     c))
                 []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn schema
  [conn]
  (let [selector '[* {:field/_parent
                      [* {:field/type [*]}]}]
        eids (-> (q/q '[:find ?e
                        :where
                        [?e :postgres/tag true]
                        [?e :type/nature :user]
                        (not [?e :type/interface true])
                        (not [?e :type/union true])]
                      @conn)
                 vec flatten)
        types (->> eids
                   (d/pull-many @conn selector)
                   (sort-by :type/name))]
    (->> types
         (reduce (fn [c t]
                   (concat c (get-type t)))
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


