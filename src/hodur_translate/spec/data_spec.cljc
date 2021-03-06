(ns hodur-translate.spec.data-spec
  (:require
    [clojure.spec.alpha :as s]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select transform setval]])
    [hodur-translate.spec.spec-schema :as spec]
    [medley.core :as medley]
    [clojure.string :as string]))


(def clojure-def 'def)

(def data-spec-symbol 'spec-tools.data-spec/spec)
(def data-spec-or 'spec-tools.data-spec/or)
(def data-spec-maybe 'spec-tools.data-spec/maybe)
(def data-spec-req 'spec-tools.data-spec/req)
(def data-spec-opt 'spec-tools.data-spec/opt)

(def spec-keys-symbol 'clojure.spec.alpha/keys)
(def spec-or 'clojure.spec.alpha/or)
(def spec-nilable 'clojure.spec.alpha/nilable)
(def spec-req-key :req-un)
(def spec-opt-key :opt-un)
(def data-req-map {:opt-un data-spec-opt}) ;;;  req default true

(defn switch-symbol
  [m from to]
  (sp/setval [(sp/walker #(= from %))] to m))


(defn data-key-sym
  ([k sym]
   (if sym
     (list sym k)
     k)))


(defn data-spec-sym
  [m sym]
  (sp/transform [sp/MAP-KEYS] #(data-key-sym % sym) m))


(defn unqualify-if
  ([m qualify?]
   (if qualify?
     m
     (medley/map-keys #(keyword (name %)) m)))
  ([m]
   (unqualify-if m false)))


(defn process-fields
  ([m fields qualify?]
   (let [[req-key field-values] fields
         req-sym (get data-req-map req-key)
         v->m (fn [v]
                (->> (map #(hash-map % (get m %)) v)
                     (apply merge)))]
     (-> (v->m field-values)
         (unqualify-if qualify?)
         (data-spec-sym req-sym))))
  ([m fields]
   (process-fields m fields false)))


(defn update-spec-keys
  ([m s qualify?]
   (let [s-vec (partition 2 (rest s))]
     (->> s-vec
          (map #(process-fields m % qualify?))
          (apply merge))))
  ([m s]
   (update-spec-keys m s false)))


(defn transform-vec
  ([m qualify?]
   (sp/transform [(sp/walker #(and (seq? %) (= spec-keys-symbol (first %))))] #(update-spec-keys m % qualify?) m))
  ([m]
   (transform-vec m false)))


(defn ->data-spec
  ([m qualify?]
   (let [transformed (-> m
                         (switch-symbol spec-nilable data-spec-maybe)
                         (transform-vec qualify?))]
     (unqualify-if (->> (sp/select [sp/ALL #(map? (val %))] transformed)
                        (into {}))
                   qualify?)))
  ([m]
   (->data-spec m false)))


(defn generate-data
  [m mk]
  (let [mkm (get m mk)]
    (list clojure-def (symbol mk) mkm)))


(defn generate-spec
  [mk]
  (let [data-name (symbol mk)
        spec-name (symbol (str "spec-" data-name))
        key-name (symbol (str "::" data-name))]
    (list clojure-def spec-name
          (list data-spec-symbol key-name data-name))))


(defn spec->data-spec
  ([m qualify?]
   (let [spec-map (->data-spec m qualify?)
         spec-map-keys (keys spec-map)]
     (->> (map #(list (generate-data spec-map %)
                      (generate-spec %)) spec-map-keys)
          (reduce concat))))
  ([m]
   (spec->data-spec m false)))


(defn schema
  ([meta-db qualify?]
   (let [raw-map (->> (spec/schema meta-db)
                      (map rest)
                      (map #(apply hash-map %))
                      (apply merge))]
     (spec->data-spec raw-map qualify?)))
  ([meta-db]
   (schema meta-db false)))

(defn spec-name?
  [v]
  (-> v
      last
      first
      (= 'spec-tools.data-spec/spec)))

(defn take-field-key
  [k-or-opt]
  (if (keyword? k-or-opt)
    [k-or-opt]
    [(last k-or-opt) {:optional 'true}]))

(defn take-field-spec
  [fspec]
  (let [malli (if (symbol? fspec)
                fspec
                (last fspec))
        remove-namespace (fn [x]
                           (-> x name
                               (string/split #"/")
                               last
                               symbol))]
    (cond
      (= 'java-time/local-date? malli) 'local-date
      (= 'java-time/local-date-time? malli) 'local-date-time
      :else (remove-namespace malli))))

(defn data-field->malli
  [entry]
  (let [k-or-opt (-> (key entry)
                     take-field-key)
        fspec (-> (val entry)
                  take-field-spec)]
    (if (second k-or-opt)
      (->> (concat k-or-opt [[:maybe fspec]])
           vec)
      (->> (concat k-or-opt [fspec])
           vec))))

(defn data->malli
  [data qualify?]
  (let [[_ mname spec-map] data
        qualify-fn (fn [v]
                     (update v 0 #(keyword (name mname) (name %))))
        qualify-malli (fn [v]
                        (if qualify?
                          (map qualify-fn v)
                          v))
        malli-map (->> (map data-field->malli spec-map)
                       qualify-malli
                       (sort-by first)
                       (into [:map]))]
    (hash-map (-> mname name keyword) malli-map)))

(defn data-spec->malli
  [data-spec qualify?]
  (let [useful-spec (filter (complement spec-name?) data-spec)]
    (map #(data->malli % qualify?) useful-spec)))

(defn malli-spec
  ([meta-db qualify?]
   (let [malli-maps (-> (schema meta-db qualify?)
                        (data-spec->malli qualify?))]
     (apply merge malli-maps)))
  ([meta-db]
   (malli-spec meta-db false)))

;;; 目前先用這種笨方法轉換成malli格式，如有時間再研究直接由datascript轉換

