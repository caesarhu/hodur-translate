(ns hodur-translate.translate
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string]]
    [clojure.spec.alpha :as s]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select transform]])
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [hodur-translate.bimap :as bi]
    [hodur-translate.utils :as utils]))


(defn get-field-translate-data
  [id-map]
  (let [{:keys [type/name translate/chinese field/_parent]} id-map
        field-fn (fn [field f-key]
                   (->> (f-key field)
                        ->kebab-case-string
                        (keyword name)))
        fields (map #(hash-map (field-fn % :field/name)
                               (->> (:translate/chinese %)
                                    ->kebab-case-string
                                    (keyword chinese)))
                    _parent)]
    (reduce merge {(keyword name) (keyword chinese)} fields)))


(defn ->name-map
  [conn]
  (let [selector '[* {:field/_parent [:field/name :translate/chinese]}]
        eids (utils/user-eids conn :translate/tag)
        id-maps (d/pull-many @conn selector eids)]
    (->> (map get-field-translate-data id-maps)
         (apply merge))))


(defn dict-bimap
  [conn]
  (let [name-map (->name-map conn)]
    [name-map (clojure.set/map-invert name-map)]))


(defn translate
  [dic name]
  (or (bi/get-value dic name)
      (bi/get-key dic name)))
