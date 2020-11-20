(ns hodur-translate.core
  (:require
    [hodur-translate.data-spec :as ds]
    [hodur-translate.engine :as engine]
    [hodur-translate.postgres-schema :as ps]
    [hodur-translate.spec-schema :as ss]
    [hodur-translate.translate :as translate]))


(def init-schema engine/init-db)
(def init-db engine/init-db)
(def dict-bimap translate/dict-bimap)
(def dict-translate translate/translate)
(def spec-schema ss/schema)
(def spec->data-spec ds/spec->data-spec)
(def data-spec-schema ds/schema)
(def postgres-schema ps/schema)
(def postgres-save-schema-sql ps/save-schema-sql)
(def postgres-save-sql ps/save-db-sql)
