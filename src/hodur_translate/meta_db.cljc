(ns hodur-translate.meta-db)


(def meta-schema
  '[^{:lacinia/tag true
      :postgres/tag true
      :spec/tag true
      :translate/tag true}
    default

    ^{:translate/chinese "性別類別"
      :enum true
      :postgres/table-order 1}
    gender-type
    [男 女]

    ^{:translate/chinese "間直接類別"
      :enum true
      :postgres/table-order 2}
    direct-type
    [直接 間接]

    ^{:translate/chinese "薪資計算類別"
      :enum true
      :postgres/table-order 3}
    salary-type
    [月薪 計時 計件]

    ^{:translate/chinese "員工類別"
      :enum true
      :postgres/table-order 4}
    employee-type
    [回聘 契約正式 契約工讀 計時 計件]

    ^{:translate/chinese "費用類別"
      :enum true
      :postgres/table-order 5}
    price-type
    [直接費用 間接費用 研發費用 推銷費用 管理費用 製造費用]

    ^{:translate/chinese "員工異動類別"
      :enum true
      :postgres/table-order 6}
    employee-change-type
    [到職 離職 復職 停職]

    ^{:translate/chinese "月薪期別"
      :enum true
      :postgres/table-order 7}
    month-type
    [上期 下期]

    ^{:translate/chinese "員工質本資料表"
      :postgres/table-order 100}
    employee
    [^{:type Integer
       :postgres/auto-increment true
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "員工質本資料表id"} id
     ^{:type String
       :postgres/index true
       :postgres/unique true
       :translate/chinese "身分證號"} taiwan-id
     ^{:type String
       :postgres/index true
       :postgres/unique true
       :translate/chinese "員工編號"} company-id
     ^{:type String
       :postgres/index true
       :translate/chinese "姓名"} name
     ^{:type Date
       :translate/chinese "生日"} birthday
     ^{:type gender-type
       :translate/chinese "性別"} gender
     ^{:type direct-type
       :translate/chinese "間直接"} direct-kind
     ^{:type salary-type
       :translate/chinese "薪資類別"} salary-kind
     ^{:type employee-type
       :translate/chinese "員工類別"} employee-kind
     ^{:type price-type
       :translate/chinese "費用類別"} price-kind
     ^{:type String
       :translate/chinese "戶籍地址"} reg-addr
     ^{:type String
       :optional true
       :translate/chinese "通訊地址"} mail-addr
     ^{:type String
       :translate/chinese "單位代號"} unit-id
     ^{:type String
       :translate/chinese "銀行代號"} bank-id
     ^{:type String
       :translate/chinese "銀行帳號"} account]

    ^{:translate/chinese "銀行一覽表"
      :postgres/table-order 200}
    bank
    [^{:type Integer
       :postgres/auto-increment true
       :postgres/primary-key true
       :spec/override clojure.core/pos-int?
       :translate/chinese "銀行對照檔id"} id
     ^{:type String
       :postgres/index true
       :translate/chinese "總行代號"} head-bank-no
     ^{:type String
       :postgres/index true
       :translate/chinese "銀行代號"} bank-no
     ^{:type String
       :translate/chinese "機構名稱"} name
     ^{:type String
       :translate/chinese "地址"} addr
     ^{:type String
       :translate/chinese "電話"} phone
     ^{:type String
       :optional true
       :translate/chinese "負責人"} principal
     ^{:type Date
       :optional true
       :translate/chinese "異動日期"} change-date]])
