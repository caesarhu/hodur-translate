(ns hodur-translate.config
  (:require [juxt.clip.repl :refer [start stop set-init! reset system]]
            [aero.core :refer [read-config]]
            [clojure.java.io :as io]))

(defn config
  ([profile]
   (read-config (io/resource "hodur_translate/clip.edn") {:profile profile}))
  ([]
   (config :dev)))