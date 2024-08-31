(ns dev.inspect.inspector
  (:require
    [mount.core :as mount]
    [portal.api :as portal]))


(mount/defstate inspector
  :start (portal/open {:app false})
  :stop (portal/close))
