(ns hodur-translate.data-spec
  (:require
    [clojure.spec.alpha :as s]
    [cjsauer.disqualified :refer [qualify-map unqualify-map]]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select transform setval]])))

(def data-spec-symbol 'spec-tools.data-spec/spec)
(def data-spec-or 'spec-tools.data-spec/or)
(def data-spec-maybe 'spec-tools.data-spec/maybe)
(def data-spec-req 'spec-tools.data-spec/req)
(def data-spec-opt 'spec-tools.data-spec/opt)

(def spec-keys-symbol 'clojure.spec.alpha/keys)
(def spec-or 'clojure.spec.alpha/or)
(def spec-maybe 'clojure.spec.alpha/nilable)
(def spec-req-key :req-un)
(def spec-opt-key :opt-un)
(def data-req-map {:opt-un data-spec-opt}) ;;; 預設req

(defn switch-symbol [m from to]
  (sp/setval [(sp/walker #(= from %))] to m))

(defn data-key-sym
  ([k sym]
   (if sym
     (->> (str "(" sym " " k ")")
          symbol)
     k)))

(defn data-spec-sym [m sym]
  (sp/transform [sp/MAP-KEYS] #(data-key-sym % sym) m))

(defn process-field [m fields]
  (let [[req-key field-values] fields
        req-sym (get data-req-map req-key)
        v->m (fn [v]
               (->> (map #(hash-map % (get m %)) v)
                    (apply merge)))]
    (-> (v->m field-values)
        (data-spec-sym req-sym))))

(defn update-spec-keys [m s]
  (let [s-vec (partition 2 (rest s))]
    (->> s-vec
         (map #(process-field m %))
         (apply merge))))

(defn transform-vec [m]
  (sp/transform [(sp/walker #(and (seq? %) (= spec-keys-symbol (first %))))] #(update-spec-keys m %) m))