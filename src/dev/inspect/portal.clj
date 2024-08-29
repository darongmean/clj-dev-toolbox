(ns dev.inspect.portal
  (:require
   [mount.core :as mount]
   [portal.api :as p]))


(mount/defstate inspector
  :start (p/open {:app false})
  :stop (p/close))
