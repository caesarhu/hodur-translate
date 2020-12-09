(ns hodur-translate.config
  (:require [juxt.clip.repl :refer [start stop set-init! reset system]]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(defmethod aero/reader 'format
  [opts tag value]
  (let [[fmt & args] value]
    (apply #?(:clj format :cljs gstring/format) fmt
           (map str args))))

(defn config
  ([profile]
   (aero/read-config (io/resource "hodur_translate/config.edn") {:profile profile}))
  ([]
   (config :dev)))