;;; Hodur Engine origin schema
(ns hodur-translate.meta-db
  (:require
    [hodur-translate.engine :as engine]
    [hodur-translate.utils :as utils]))


(def meta-schema
  '[^{:lacinia/tag true
      :postgres/tag true
      :spec/tag true
      :translate/tag true}
    default

    ^{:translate/chinese "危安物品檔"}
    items
    [^{:type Integer
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
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
       :optional true
       :translate/chinese "飛機班次"} flight
     ^{:type String
       :translate/chinese "單位"} unit
     ^{:type String
       :optional true
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
     ^{:type String
       :optional true
       :translate/chinese "輸入設備IP"} ip
     ^{:type String
       :optional true
       :translate/chinese "備註"} memo]

    ^{:translate/chinese "紀錄清單檔"}
    all-list
    [^{:type Integer
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "紀錄清單檔id"} id
     ^{:type items
       :postgres/index true
       :cardinality [0 1]
       :spec/override clojure.core/pos-int?
       :postgres/ref-update :cascade
       :postgres/ref-delete :cascade
       :translate/chinese "items-id參考"} items-id
     ^{:type String
       :postgres/index true
       :translate/chinese "項目"} item
     ^{:type Integer
       :translate/chinese "數量"} quantity]

    ^{:translate/chinese "項目清單檔"}
    item-list
    [^{:type Integer
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "項目清單檔id"} id
     ^{:type items
       :postgres/index true
       :cardinality [0 1]
       :spec/override clojure.core/pos-int?
       :postgres/ref-update :cascade
       :postgres/ref-delete :cascade
       :translate/chinese "items-id參考"} items-id
     ^{:type String
       :translate/chinese "種類"} kind
     ^{:type String
       :translate/chinese "類別"} subkind
     ^{:type String
       :translate/chinese "物品"} object]

    ^{:translate/chinese "項目人數檔"}
    item-people
    [^{:type Integer
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "項目人數檔id"} id
     ^{:type items
       :postgres/index true
       :cardinality [0 1]
       :spec/override clojure.core/pos-int?
       :postgres/ref-update :cascade
       :postgres/ref-delete :cascade
       :translate/chinese "items-id參考"} items-id
     ^{:type String
       :translate/chinese "種類"} kind
     ^{:type Integer
       :translate/chinese "件數"} piece
     ^{:type Integer
       :translate/chinese "人數"} people]

    ^{:translate/chinese "單位檔"}
    units
    [^{:type Integer
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "單位檔id"} id
     ^{:type String
       :translate/chinese "單位"} unit
     ^{:type String
       :translate/chinese "子單位"} subunit]

    ^{:translate/chinese "檔案最後時間檔"}
    last-time
    [^{:type DateTime
       :postgres/primary-key true
       :translate/chinese "最後時間"} file-time
     ^{:type Integer
       :translate/chinese "全部處理紀錄"} total
     ^{:type Integer
       :translate/chinese "處理成功紀錄"} success
     ^{:type Integer
       :translate/chinese "處理失敗紀錄"} fail]

    ^{:translate/chinese "郵件列表檔"}
    mail-list
    [^{:type Integer
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "郵件列表id"} id
     ^{:type String
       :translate/chinese "單位"} unit
     ^{:type String
       :translate/chinese "子單位"} subunit
     ^{:type String
       :optional true
       :translate/chinese "職稱"} position
     ^{:type String
       :optional true
       :translate/chinese "姓名"} name
     ^{:type String
       :optional true
       :translate/chinese "備註"} memo]])


(def meta-db
  (engine/init-db meta-schema))
