(ns hodur-translate.data-spec
  (:require
    [clojure.spec.alpha :as s]
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

(defn switch-symbol [m from to]
  (sp/setval [(sp/putval 2) (sp/walker #(= from %))] to m))

(defn transform-vec [m]
  (sp/select [(sp/walker #(and (seq? %) (= spec-keys-symbol (first %))))] m))