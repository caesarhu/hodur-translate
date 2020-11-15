;;; Hodur Engine origin schema
(ns hodur-translate.meta-db
  (:require
    [hodur-engine.core :as hodur]))


(def engine-schema
  '[^{:lacinia/tag true
      :datomic/tag true
      :spec/tag true
      :translate/tag true}
    default

    ^{:translate/chinese "危安物品檔"}
    items
    [^{:type Integer
       :datomic/unique :db.unique/identity
       :translate/chinese "危安物品id"} id
     ^{:type String
       :translate/chinese "原始檔案"} file
     ^{:type DateTime
       :translate/chinese "原始檔案時間"} file-time
     ^{:type String
       :translate/chinese "攜帶方式"} carry
     ^{:type DateTime
       :translate/chinese "查獲時間"} check-time
     ^{:type String
       :translate/chinese "飛機班次"} flight
     ^{:type String
       :translate/chinese "單位"} unit
     ^{:type String
       :translate/chinese "子單位"} subunit
     ^{:type String
       :translate/chinese "查獲員警"} police
     ^{:type String
       :translate/chinese "處理方式"} process
     ^{:type String
       :translate/chinese "查獲人簽名"} check-sign
     ^{:type String
       :optional true
       :translate/chinese "旅客簽名"} passenger-sign
     ^{:type String
       :optional true
       :translate/chinese "貨運業者簽名"} trader-sign
     ^String ip
     ^{:type String
       :optional true
       :translate/chinese "備註"} memo]

    ^{:translate/chinese "郵件列表檔"}
    mail-list
    [^{:type Integer
       :datomic/unique :db.unique/identity
       :translate/chinese "郵件列表id"} id
     ^{:type String
       :translate/chinese "單位"} unit
     ^{:type String
       :translate/chinese "子單位"} subunit
     ^{:type String
       :translate/chinese "職稱"} position
     ^{:type String
       :translate/chinese "姓名"} name
     ^{:type String
       :optional true
       :translate/chinese "備註"} memo]

    ^{:translate/chinese "測試資料"}
    test-data
    [^{:type Integer
       :datomic/unique :db.unique/identity
       :translate/chinese "測試id"} id
     ^{:type Date
       :translate/chinese "日期"} test-date]])


(def meta-db
  (hodur/init-schema engine-schema))
