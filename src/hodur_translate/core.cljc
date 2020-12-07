(ns hodur-translate.core
  (:require
    [hodur-translate.data-spec :as ds]
    [hodur-translate.engine :as engine]
    [hodur-translate.postgres-schema :as ps]
    [hodur-translate.spec-schema :as ss]
    [hodur-translate.translate :as translate]
    [hodur-translate.utils :as utils]))


(defn read-schema
  [path]
  (utils/read-schema path))


(defn spit-code
  ([file obj-v opts]
   (utils/spit-code file obj-v opts))
  ([file obj-v]
   (spit-code file obj-v nil)))


(defn init-schema
  [source-schema & others]
  (engine/init-db source-schema others))


(defn init-db
  [source-schema & others]
  (engine/init-db source-schema others))


(defn dict-bimap
  [conn]
  (translate/dict-bimap conn))


(defn dict-translate
  [dic name]
  (translate/translate dic name))


(defn spec-schema
  ([conn]
   (ss/schema conn))
  ([conn opts]
   (ss/schema conn opts)))


(defn spec->data-spec
  ([m qualify?]
   (ds/spec->data-spec m qualify?))
  ([m]
   (ds/spec->data-spec m)))


(defn data-spec-schema
  ([m qualify?]
   (ds/schema m qualify?))
  ([m]
   (ds/schema m)))


(defn spit-data-spec
  ([path conn qualify?]
   (let [result (ds/schema conn qualify?)]
     (utils/spit-code path result)))
  ([path conn]
   (spit-data-spec path conn nil)))

(defn spit-malli-spec
  ([path conn qualify?]
   (let [result (ds/malli-spec conn qualify?)]
     (utils/spit-code path result)))
  ([path conn]
   (spit-malli-spec path conn false)))


(defn postgres-schema
  [conn]
  (ps/schema conn))


(defn save-schema-sql
  [schema-v path]
  (ps/save-schema-sql schema-v path))


(defn spit-db-sql
  [path conn]
  (ps/save-db-sql conn path))

