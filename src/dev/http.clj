(ns dev.http
  (:require
   [hato.client :as hato]))


(defn http
  "See https://github.com/gnarroway/hato?tab=readme-ov-file#making-queries

  Ex:
  ```clj

  (http :get \"https://darongmean.com\")
  (http :post \"https://darongmean.com\")

  ```
  "
  [method url & [opts respond raise]]
  (#'hato/configure-and-execute method url opts respond raise))
