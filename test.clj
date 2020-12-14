{:all-list
 [:map
  [:all-list/id pos-int?]
  [:all-list/item string?]
  [:all-list/items-id pos-int?]
  [:all-list/quantity integer?]],
 :ipad
 [:map
  [:ipad/id pos-int?]
  [:ipad/ip string?]
  [:ipad/ipad-name string?]
  [:ipad/subunit {:optional true} [:maybe string?]]
  [:ipad/unit string?]],
 :item-people
 [:map
  [:item-people/id pos-int?]
  [:item-people/items-id pos-int?]
  [:item-people/kind string?]
  [:item-people/people integer?]
  [:item-people/piece integer?]],
 :units
 [:map
  [:units/id pos-int?]
  [:units/subunit {:optional true} [:maybe string?]]
  [:units/unit string?]],
 :mail-list
 [:map
  [:mail-list/email string?]
  [:mail-list/id pos-int?]
  [:mail-list/memo {:optional true} [:maybe string?]]
  [:mail-list/name {:optional true} [:maybe string?]]
  [:mail-list/position {:optional true} [:maybe string?]]
  [:mail-list/subunit {:optional true} [:maybe string?]]
  [:mail-list/unit string?]],
 :item-list
 [:map
  [:item-list/id pos-int?]
  [:item-list/items-id pos-int?]
  [:item-list/kind string?]
  [:item-list/object string?]
  [:item-list/subkind string?]],
 :items
 [:map
  [:items/carry string?]
  [:items/check-line {:optional true} [:maybe string?]]
  [:items/check-sign {:optional true} [:maybe string?]]
  [:items/check-time local-date-time]
  [:items/file string?]
  [:items/file-time local-date-time]
  [:items/flight {:optional true} [:maybe string?]]
  [:items/id pos-int?]
  [:items/ip {:optional true} [:maybe string?]]
  [:items/memo {:optional true} [:maybe string?]]
  [:items/passenger-id {:optional true} [:maybe string?]]
  [:items/passenger-sign {:optional true} [:maybe string?]]
  [:items/police string?]
  [:items/process string?]
  [:items/subunit {:optional true} [:maybe string?]]
  [:items/trader-sign {:optional true} [:maybe string?]]
  [:items/unit string?]],
 :last-time
 [:map
  [:last-time/fail integer?]
  [:last-time/file-time local-date-time]
  [:last-time/id pos-int?]
  [:last-time/success integer?]
  [:last-time/total integer?]]}
