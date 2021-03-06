(ns hodur-translate.postgres.postgres-format
  (:require
    [clojure.string :as string]
    [honeysql-postgres.util :as util]
    [honeysql.format :as sqlf :refer [fn-handler format-clause format-modifiers]]
    [honeysql-postgres.format :as psqlf]
    [honeysql.helpers :as sqlh #?(:clj :refer :cljs :refer-macros) [defhelper]]))


(defn quotation-str
  [s]
  (str "'" s "'"))

(defmethod fn-handler "unique" [_ & args]
  (let [flag (first args)]
    (if (true? flag)
      (str "UNIQUE")
      (when flag
        (str "UNIQUE" (util/comma-join-args args))))))


(defmethod fn-handler "primary-key" [_ & args]
  (let [flag (first args)]
    (if (true? flag)
      (str "PRIMARY KEY")
      (when flag
        (str "PRIMARY KEY" (util/comma-join-args args))))))

(defmethod fn-handler "references" [_ reftable]
  (str "REFERENCES " (sqlf/to-sql reftable)))

(defmethod fn-handler "identity-always" [_ flag?]
  (when flag?
    (str "GENERATED ALWAYS AS IDENTITY")))


(defmethod fn-handler "identity-default" [_ flag?]
  (when flag?
    (str "GENERATED BY DEFAULT AS IDENTITY")))


(defmethod fn-handler "on-update" [_ value]
  (str "ON UPDATE " (sqlf/to-sql value)))


(defmethod fn-handler "on-delete" [_ value]
  (str "ON DELETE " (sqlf/to-sql value)))


(defmethod fn-handler "generate-column" [_ function]
  (str "GENERATED ALWAYS AS " (sqlf/paren-wrap function) " STORED"))


(defmethod fn-handler "optional" [_ optional?]
  (when-not optional?
    (str "NOT NULL")))


(defmethod fn-handler "not-null" [_ flag?]
  (when flag?
    (str "NOT NULL")))


(defmethod format-clause :create-enum [[_ [enum-name enum-values]] _]
  (let [values-str (util/comma-join-args enum-values)]
    (str "CREATE TYPE " (-> enum-name
                            util/get-first
                            sqlf/to-sql) " AS ENUM" values-str)))


(defhelper create-enum [m enum-name]
  (assoc m :create-enum (sqlh/collify enum-name)))


(defmethod format-clause :drop-enum [[_ params] _]
  (let [[if-exists & others] params
        types (if-not (= :if-exists if-exists)
                params
                others)]
    (str "DROP TYPE "
         (when (= :if-exists if-exists) "IF EXISTS ")
         (->> types
              (map sqlf/to-sql)
              sqlf/comma-join))))


(defhelper drop-enum [m enum-name]
  (assoc m :drop-enum (sqlh/collify enum-name)))


(defmethod format-clause :create-index [[_ params] _]
  (let [[unique & others] params
        [index-name table-name & columns] (if-not (= :unique unique)
                                            params
                                            others)]
    (str "CREATE "
         (when (= :unique unique)
           "UNIQUE ")
         "INDEX "
         (sqlf/to-sql index-name)
         " ON "
         (sqlf/to-sql table-name)
         " "
         (->> columns
              (map sqlf/to-sql)
              sqlf/comma-join
              sqlf/paren-wrap))))


(defhelper create-index [m params]
  (assoc m :create-index (apply sqlh/collify params)))


(defmethod format-clause :drop-index [[_ params] _]
  (let [[if-exists & others] params
        indexs (if-not (= :if-exists if-exists)
                 params
                 others)]
    (str "DROP INDEX "
         (when (= :if-exists if-exists) "IF EXISTS ")
         (->> indexs
              (map sqlf/to-sql)
              sqlf/comma-join))))


(defhelper drop-index [m index-name]
  (assoc m :drop-index (sqlh/collify index-name)))
