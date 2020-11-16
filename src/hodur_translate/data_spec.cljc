(ns hodur-translate.data-spec
  (:require
    [clojure.spec.alpha :as s]
    #?(:clj  [com.rpl.specter :as sp]
       :cljs [com.rpl.specter :as s :refer-macros [select transform]])))

(def data-spec-symbol 'spec-tools.data-spec/spec)