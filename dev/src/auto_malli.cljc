(ns auto-malli)

(def malli-bank
  [:map
   [:name clojure.core/string?]
   [:head-id clojure.core/string?]
   [:bank-id clojure.core/string?]
   [:principal {:optional true} [:maybe clojure.core/string?]]
   [:phone {:optional true} [:maybe clojure.core/string?]]
   [:change-date {:optional true} [:maybe local-date]]
   [:addr {:optional true} [:maybe clojure.core/string?]]])


(def malli-employee
  [:map
   [:education {:optional true} [:maybe clojure.core/string?]]
   [:work-place {:optional true} [:maybe clojure.core/string?]]
   [:account {:optional true} [:maybe clojure.core/string?]]
   [:employee-kind {:optional true} [:maybe clojure.core/string?]]
   [:name clojure.core/string?]
   [:job-title-2 {:optional true} [:maybe clojure.core/string?]]
   [:birthday {:optional true} [:maybe java-time/local-date?]]
   [:phone {:optional true} [:maybe clojure.core/string?]]
   [:exception {:optional true} [:maybe clojure.core/string?]]
   [:mobile {:optional true} [:maybe clojure.core/string?]]
   [:bank-id {:optional true} [:maybe clojure.core/string?]]
   [:price-kind {:optional true} [:maybe clojure.core/string?]]
   [:company-id clojure.core/string?]
   [:job-title {:optional true} [:maybe clojure.core/string?]]
   [:salary-kind {:optional true} [:maybe clojure.core/string?]]
   [:factory {:optional true} [:maybe clojure.core/string?]]
   [:taiwan-id clojure.core/string?]
   [:unit-id {:optional true} [:maybe clojure.core/string?]]
   [:id clojure.core/pos-int?]
   [:reg-addr {:optional true} [:maybe clojure.core/string?]]
   [:mail-addr {:optional true} [:maybe clojure.core/string?]]
   [:gender {:optional true} [:maybe clojure.core/string?]]
   [:education-period {:optional true} [:maybe clojure.core/string?]]
   [:direct-kind {:optional true} [:maybe clojure.core/string?]]
   [:memo {:optional true} [:maybe clojure.core/string?]]])


(def malli-employee-change
  [:map
   [:change-day java-time/local-date?]
   [:employee-id clojure.core/pos-int?]
   [:change-kind clojure.core/string?]
   [:change-at java-time/local-date-time?]
   [:id clojure.core/pos-int?]
   [:memo {:optional true} [:maybe clojure.core/string?]]])


