(ns dev
  (:require
    [hodur-translate.utils :as utils]
    [fipp.edn :refer [pprint]]
    [juxt.clip.repl :refer [start stop set-init! reset system]]
    [hodur-translate.config :refer [config]]
    [hodur-translate.core :refer :all]
    [migratus.core :as migratus]
    [malli.core :as m]
    [hodur-translate.data-spec :as ds]
    [com.rpl.specter :as sp]))

(set-init! (fn [] (config :dev)))

(comment
  (start)
  (reset)
  (stop)
  system)

(defn meta-db
  []
  (init-db (utils/read-schema (:schema-path (config :dev)))))

(defn migratus-config
  ([tag]
   (:migratus (config tag)))
  ([]
   (migratus-config :dev)))

(defn migrate
  ([tag]
   (migratus/migrate (migratus-config tag)))
  ([]
   (migrate :dev)))

(defn rollback
  ([tag]
   (migratus/rollback (migratus-config tag)))
  ([]
   (rollback :dev)))

