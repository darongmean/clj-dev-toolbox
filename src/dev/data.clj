(ns dev.data
  (:refer-clojure :exclude [ex-message])
  (:require
   [cheshire.core :as chesire]
   [malli.core :as m]
   [malli.error :as me]
   [malli.provider :as mp]
   [malli.transform :as mt]))


;; see https://github.com/metosin/malli?tab=readme-ov-file#development-mode
;; invoke before mp/provider call to work around random schema error messages caused by the function
((requiring-resolve 'malli.dev/stop!))

(def provider (mp/provider))
(defn infer-schema
  "Infer a Malli schema.

  Ex:

  ```clj

  (infer-schema [{:a :b}])
  (infer-schema #{{:a :b}})
  (infer-schema {:a :b})
  (infer-schema {:a :b} {:a :c})

  ```
  "
  [value & values]
  (cond
    (sequential? value) (provider value)
    (set? value) (provider value)
    :else (provider (conj values value))))

(comment
  (infer-schema [{:a :b}])
  (infer-schema #{{:a :b}})
  (infer-schema {:a :b})
  (infer-schema {:a :b} {:a :c})
  ;;;
  )

;; see https://github.com/metosin/malli?tab=readme-ov-file#development-mode
;; invoke after mp/provider call to work around random schema error messages caused by the function
((requiring-resolve 'malli.dev/start!))


(defn ex-message [ex]
  (let [explain (some-> ex
                        (ex-data)
                        (:data)
                        (:explain))]
    (when explain
      {:ex.explain/value  (:value explain)
       :ex.explain/errors (some-> explain
                                  (me/with-spell-checking)
                                  (me/humanize))})))


(defn parse
  "Ex:

  (parse [:map] {:a :b})

  ;; errors
  (parse [:map] 123)
  "
  [schema value]
  (m/coerce schema value mt/string-transformer))

(comment
  (parse [:map] {:a :b})
  ;; errors
  (parse [:map] 123)
  ;;;
  )


(defn parse-json
  "Ex:

  (parse-json [:map [:a :keyword]] {:a \"b\"})

  ;; errors
  (parse-json [:map [:a :keyword]] {\"a\" \"b\"})
  "
  [schema value]
  (m/coerce schema value mt/json-transformer))

(comment
  (parse-json [:map [:a :keyword]] {:a "b"})
  ;; errors
  (parse-json [:map [:a :keyword]] {"a" "b"})
  ;;;
  )


(defn decode-json
  "Ex:

  (decode-json \"{\\\"a\\\":\\\"b\\\"}\")
  "
  [string]
  (chesire/parse-string string))

(comment
  (decode-json "{\"a\":\"b\"}")
  ;;;
  )


(defn encode-json
  "Ex:

  (encode-json {:a :b})
  "
  [value]
  (chesire/generate-string value))

(comment
  (encode-json {:a :b})
  ;;;
  )
