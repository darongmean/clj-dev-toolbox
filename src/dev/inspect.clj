(ns dev.inspect
  (:require
    [dev.inspect.inspector]
    [dev.system :as system]
    [portal.api :as portal]))


(defn *v
  "Current value of selected item in the portal inspector."
  []
  (deref dev.inspect.inspector/inspector))


(defn inspect
  [value]
  (system/start #'dev.inspect.inspector/inspector)
  (portal/submit value)
  value)


(defn inspect-table
  [value]
  (inspect (with-meta value {:portal.viewer/default :portal.viewer/table})))


(defn inspect-diff2
  [& values]
  (inspect (with-meta values {:portal.viewer/default :portal.viewer/diff})))


(defn inspect-diff
  ([[left right]]
   (inspect-diff left right))
  ([left right]
   (tagged-literal 'cursive/diff {:left left :right right :title "cursive/diff"})))
