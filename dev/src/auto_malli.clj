(ns auto-malli
  (:require [hodur-translate.spec.malli-schemas :refer [local-date local-date-time]]))

(def malli-bank
  [:map
   [:addr {:optional true} [:maybe clojure.core/string?]]
   [:bank-id clojure.core/string?]
   [:change-date {:optional true} [:maybe local-date]]
   [:head-id clojure.core/string?]
   [:name clojure.core/string?]
   [:phone {:optional true} [:maybe clojure.core/string?]]
   [:principal {:optional true} [:maybe clojure.core/string?]]])


(def malli-employee
  [:map
   [:account {:optional true} [:maybe clojure.core/string?]]
   [:bank-id {:optional true} [:maybe clojure.core/string?]]
   [:birthday {:optional true} [:maybe local-date]]
   [:company-id clojure.core/string?]
   [:direct-kind {:optional true} [:maybe clojure.core/string?]]
   [:education {:optional true} [:maybe clojure.core/string?]]
   [:education-period {:optional true} [:maybe clojure.core/string?]]
   [:employee-kind {:optional true} [:maybe clojure.core/string?]]
   [:exception {:optional true} [:maybe clojure.core/string?]]
   [:factory {:optional true} [:maybe clojure.core/string?]]
   [:gender {:optional true} [:maybe clojure.core/string?]]
   [:id clojure.core/pos-int?]
   [:job-title {:optional true} [:maybe clojure.core/string?]]
   [:job-title-2 {:optional true} [:maybe clojure.core/string?]]
   [:mail-addr {:optional true} [:maybe clojure.core/string?]]
   [:memo {:optional true} [:maybe clojure.core/string?]]
   [:mobile {:optional true} [:maybe clojure.core/string?]]
   [:name clojure.core/string?]
   [:phone {:optional true} [:maybe clojure.core/string?]]
   [:price-kind {:optional true} [:maybe clojure.core/string?]]
   [:reg-addr {:optional true} [:maybe clojure.core/string?]]
   [:salary-kind {:optional true} [:maybe clojure.core/string?]]
   [:taiwan-id clojure.core/string?]
   [:unit-id {:optional true} [:maybe clojure.core/string?]]
   [:work-place {:optional true} [:maybe clojure.core/string?]]])


(def malli-employee-change
  [:map
   [:change-at local-date-time]
   [:change-day local-date]
   [:change-kind clojure.core/string?]
   [:employee-id clojure.core/pos-int?]
   [:id clojure.core/pos-int?]
   [:memo {:optional true} [:maybe clojure.core/string?]]])


