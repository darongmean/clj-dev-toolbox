(ns dev.inspect
  (:require
    [clojure.pprint :as pprint]
    [dev.data :as data]
    [dev.inspect.inspector]
    [dev.system :as system]
    [portal.api :as portal]))


(defn *v
  "Current value of selected item in the portal inspector."
  []
  (deref dev.inspect.inspector/inspector))


(defn inspect
  ([viewer value]
   (let [kw (keyword "portal.viewer" (name viewer))
         v  (if (string? value)
              (with-meta
                [:portal.viewer/text value]
                {:portal.viewer/default :portal.viewer/hiccup})
              (with-meta value {:portal.viewer/default kw}))]
     (system/start #'dev.inspect.inspector/inspector)
     (portal/submit v)
     value))
  ([value]
   (inspect :hiccup [:portal.viewer/code (with-out-str (data/pprint value))])
   value))


(defn inspect-table
  ([rows]
   (inspect :hiccup [:portal.viewer/code (with-out-str (pprint/print-table rows))])
   rows)
  ([headers rows]
   (inspect :hiccup [:portal.viewer/code (with-out-str (pprint/print-table headers rows))])
   rows))


(defn inspect-diff2
  [& values]
  (inspect :diff values))


(defn inspect-diff
  ([[left right]]
   (inspect-diff left right))
  ([left right]
   (tagged-literal 'cursive/diff {:left left :right right :title "cursive/diff"})))
