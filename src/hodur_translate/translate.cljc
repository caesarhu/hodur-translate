(ns hodur-translate.translate
  (:require
    [camel-snake-kebab.core :refer [->kebab-case-string]]
    [clojure.spec.alpha :as s]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select transform]])
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [hodur-translate.bimap :as bi]))


(defn get-eids
  [conn]
  (-> (q/q '[:find ?e
             :where
             [?e :translate/tag true]
             [?e :type/nature :user]
             (not [?e :type/interface true])
             (not [?e :type/union true])]
           @conn)
      vec flatten))


(defn get-types
  [conn]
  (let [selector '[* {:field/_parent
                      [* {:field/type [*]}]}]
        eids (get-eids conn)]
    (->> eids
         (d/pull-many @conn selector)
         (sort-by :type/name))))


(def translate-field
  [:field/name :translate/chinese])


(defn get-translate-field
  [m]
  (let [entity (sp/select (sp/submap translate-field) m)
        fields (sp/select [sp/ALL (sp/submap translate-field)] (:field/_parent m))]
    {:entity entity
     :fields fields}))


(defn process-entity
  [{:keys [type/name translate/chinese field/_parent]}]
  (let [entity-name (->kebab-case-string name)
        entity-chinese (->kebab-case-string chinese)
        fields (->> (sp/select [sp/ALL #(:translate/tag %) (sp/submap translate-field) sp/MAP-VALS] _parent)
                    (sp/transform [sp/ALL] ->kebab-case-string)
                    (partition 2))
        dic-map {(keyword entity-name) (keyword entity-chinese)}]
    (reduce (fn [m v]
              (merge m
                     {(keyword entity-name (first v)) (keyword entity-chinese (last v))}))
            dic-map fields)))


(defn ->name-map
  [conn]
  (let [eids (get-types conn)]
    (reduce (fn [m e]
              (merge m (process-entity e)))
            {} eids)))


(defn ->dic-bimap
  [conn]
  (let [name-map (->name-map conn)]
    [name-map (clojure.set/map-invert name-map)]))


(defn translate
  [dic name]
  (or (bi/get-value dic name)
      (bi/get-key dic name)))
