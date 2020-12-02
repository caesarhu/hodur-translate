(def bank
  {:name clojure.core/string?,
   :head-id clojure.core/string?,
   :bank-id clojure.core/string?,
   (spec-tools.data-spec/opt :principal)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :phone)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :change-date)
   (spec-tools.data-spec/maybe java-time/local-date?),
   (spec-tools.data-spec/opt :addr)
   (spec-tools.data-spec/maybe clojure.core/string?)})


(def spec-bank (spec-tools.data-spec/spec ::bank bank))


(def employee
  {(spec-tools.data-spec/opt :education)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :work-place)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :account)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :employee-kind) clojure.core/string?,
   :name clojure.core/string?,
   (spec-tools.data-spec/opt :job-title-2)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :birthday)
   (spec-tools.data-spec/maybe java-time/local-date?),
   (spec-tools.data-spec/opt :phone)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :exception)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :mobile)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :bank-id) clojure.core/string?,
   (spec-tools.data-spec/opt :price-kind) clojure.core/string?,
   :company-id clojure.core/string?,
   (spec-tools.data-spec/opt :job-title)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :salary-kind) clojure.core/string?,
   (spec-tools.data-spec/opt :factory)
   (spec-tools.data-spec/maybe clojure.core/string?),
   :taiwan-id clojure.core/string?,
   (spec-tools.data-spec/opt :unit-id)
   (spec-tools.data-spec/maybe clojure.core/string?),
   :id clojure.core/pos-int?,
   (spec-tools.data-spec/opt :reg-addr)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :mail-addr)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :gender) clojure.core/string?,
   (spec-tools.data-spec/opt :education-period)
   (spec-tools.data-spec/maybe clojure.core/string?),
   (spec-tools.data-spec/opt :direct-kind) clojure.core/string?,
   (spec-tools.data-spec/opt :memo)
   (spec-tools.data-spec/maybe clojure.core/string?)})


(def spec-employee (spec-tools.data-spec/spec ::employee employee))


(def employee-change
  {:change-day java-time/local-date?,
   :employee-id clojure.core/pos-int?,
   :change-kind clojure.core/string?,
   :change-at java-time/local-date-time?,
   :id clojure.core/pos-int?,
   (spec-tools.data-spec/opt :memo)
   (spec-tools.data-spec/maybe clojure.core/string?)})


(def spec-employee-change
  (spec-tools.data-spec/spec ::employee-change employee-change))


