(ns dev
  (:require
    [juxt.clip.repl :refer [start stop set-init! reset system]]
    [hodur-translate.config :refer [config]]))

(set-init! (fn [] (config :dev)))

(comment
  (start)
  (reset)
  (stop)
  system)

(defn meta-db
  []
  (:meta-db system))

