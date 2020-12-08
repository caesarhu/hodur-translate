(ns hodur-translate.spec.malli-schemas
  (:require [malli.core :as m]
            [java-time :as jt]))

(defn date->str
  [d]
  (if (jt/local-date? d)
    (jt/format :iso-local-date d)
    d))

(defn str->date
  [s]
  (if (string? s)
    (jt/local-date s)
    s))

(defn date-time->str
  [t]
  (if (jt/local-date-time? t)
    (jt/format :iso-local-date t)
    t))

(defn str->date-time
  [s]
  (if (string? s)
    (jt/local-date-time s)
    s))

(def local-date
  (m/-simple-schema
    {:type :local-date
     :pred jt/local-date?
     :type-properties {:error/message "should be java-time/local-date"
                       :decode/string str->date
                       :encode/string date->str
                       :decode/json str->date
                       :encode/json date->str
                       :json-schema/type "string"
                       :json-schema/format "date"}}))

(def local-date-time
  (m/-simple-schema
    {:type            :local-date-time
     :pred            jt/local-date-time?
     :type-properties {:error/message      "should be java-time/local-date-time"
                       :decode/string      str->date-time
                       :encode/string      date-time->str
                       :decode/json        str->date-time
                       :encode/json        date-time->str
                       :json-schema/type   "string"
                       :json-schema/format "date-time"}}))