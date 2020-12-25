(ns hodur-translate.translate
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string]]
    [datascript.core :as d]
    [hodur-translate.utils :as utils]))


(defn get-field-translate-data
  [type]
  (let [{:keys [type/name translate/chinese field/_parent]} type
        fields (map #(hash-map (->> (:field/name  %)
                                    ->kebab-case-string
                                    (keyword name))
                               (->> (:translate/chinese %)
                                    ->kebab-case-string
                                    (keyword chinese)))
                    (filter :translate/chinese _parent))]
    (if  chinese
      (reduce merge {(keyword name) (keyword chinese)} fields)
      (reduce merge {} fields))))

(defn ->name-map
  [conn]
  (let [selector '[* {:field/_parent [:field/name :translate/chinese]}]
        eids (utils/user-eids conn :translate/tag)
        types (d/pull-many @conn selector eids)]
    (->> (map get-field-translate-data types)
         (apply merge))))


(defn dict-bimap
  [conn]
  (let [name-map (->name-map conn)]
    (merge name-map (clojure.set/map-invert name-map))))


(defn translate
  [dict name]
  (or (get dict name)
      name))
