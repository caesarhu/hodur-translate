(ns hodur-translate.utils
  (:require
    [cljstyle.config :as config]
    [cljstyle.format.core :as cf]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.set :refer [difference union intersection]]
    [clojure.string :as string]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select select-one transform setval]])
    [datascript.core :as d]
    [datascript.query-v3 :as q]
    [cljstyle.task.core :as fix]
    [com.rpl.specter :as sp])
  (:import
    (java.io StringWriter)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def primary-types (set  ["String" "Float" "Integer" "Boolean" "Date" "DateTime" "ID"]))


(defn primary-type?
  [type]
  (contains? primary-types type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topological Sorting
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-schema
  [path]
  (->> path
       io/resource
       slurp
       read-string))


(defn pretty-format
  [obj]
  (with-open [w (StringWriter.)]
    (binding [*out* w]
      (pprint obj *out*)
      (str w))))


(def default-cljstyle
  config/default-config)

(defn cljstyle-str
  ([s opts]
   (cf/reformat-string s (merge default-cljstyle opts)))
  ([s]
   (cljstyle-str s nil)))


(defn pretty-str
  ([obj opts]
   (-> obj
       pretty-format
       (cljstyle-str opts)))
  ([obj]
   (pretty-str obj nil)))


(defn spit-code
  ([file obj-v opts]
   (let [out-v (map #(pretty-str % opts) obj-v)
         out-line (map #(vector %1 %2) out-v (repeat "\n\n"))
         out-str (->> out-line
                      vec
                      flatten
                      (apply str))]
     (spit file out-str)
     (if (fix/fix-sources [file])
       (println (str file " output success!"))
       (println (str file " output fail!")))))
  ([file obj-v]
   (spit-code file obj-v nil)))

(def all-types
  '[:find [(pull ?t [* {:type/implements [*]
                        :field/_parent
                        [* {:field/type [*]
                            :param/_parent
                            [* {:param/type [*]}]}]}]) ...]
    :where
    [?t :type/name]])


(defn get-all-types
  [conn]
  (d/q all-types @conn))


(defn user-eids
  ([conn tag]
   (if tag
     (-> (d/q '[:find [?e ...]
                :in $ ?tag
                :where
                [?e ?tag true]
                [?e :type/nature :user]
                (not [?e :type/interface true])
                (not [?e :type/union true])]
              @conn tag)
         vec flatten)
     (-> (d/q '[:find [?e ...]
                :where
                [?e :type/nature :user]
                (not [?e :type/interface true])
                (not [?e :type/union true])]
              @conn)
         vec flatten)))
  ([conn]
   (user-eids conn nil)))


(defn get-id-types
  ([conn tag]
   (let [selector '[* {:field/_parent
                       [* {:field/type [*]}]}]
         eids (user-eids conn tag)
         types (->> eids
                    (d/pull-many @conn selector)
                    (sort-by :type/name))]
     (sp/setval [sp/ALL :field/_parent sp/ALL #(not (get % tag))] sp/NONE types)))
  ([conn]
   (get-id-types conn nil)))


(defn all-ids
  "Returns all ids of all entity nodes. When an optional map is passed
  with a tag, the ids are filtered by that tag."
  ([conn]
   (all-ids conn nil))
  ([conn {:keys [tag]}]
   (if tag
     (d/q '[:find [?e ...]
            :in $ ?tag
            :where
            [?e ?tag true]]
          @conn tag)
     (d/q '[:find [?e ...]
            :where
            [?e]]
          @conn))))
