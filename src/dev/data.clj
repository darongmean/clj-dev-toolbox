(ns dev.data
  (:refer-clojure :exclude [ex-message])
  (:require
   [cheshire.core :as chesire]
   [edamame.core :as edamame]
   [lambdaisland.data-printers :as dp]
   [lambdaisland.data-printers.deep-diff2 :as dp-ddiff2]
   [lambdaisland.data-printers.puget :as dp-puget]
   [lambdaisland.data-printers.transit :as dp-transit]
   [malli.core :as m]
   [malli.error :as me]
   [malli.provider :as mp]
   [malli.transform :as mt]
   [puget.printer :as puget]
   [taoensso.truss :as truss]
   [time-literals.read-write]))


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
  "Parse values using malli.transform/string-transformer. It doesn't parse keys of maps.

  Ex:

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
  "Parse values using malli.transform/json-transformer. It doesn't parse keys of maps.

  Ex:

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


(defn read-json
  "Ex:

  (read-json \"{\\\"a\\\":\\\"b\\\"}\")
  "
  [string]
  (chesire/parse-string string true))

(comment
  (read-json "{\"a\":\"b\"}")
  ;;;
  )

(defn print-json'
  [value]
  (chesire/generate-string value))

(defn print-json
  "Use `print-json'` to generate a JSON string and check if it can be read by `read-json`.

  Ex:

  (print-json {:a :b})
  "
  [value]
  (let [string (print-json' value)]
    (truss/have! read-json string
                 :data {:msg    "JSON string generated, but failed to read-json"
                        :string string
                        :value  value})
    string))

(comment
  (print-json {:a :b})
  ;;;
  )

(defn read-edn
  "Ex:

  (read-edn \"{:a :b}\")
  "
  [string]
  (edamame/parse-string string {:readers time-literals.read-write/tags}))

(comment
  (read-edn "{:a :b}")
  ;;;
  )


(defn pprint [value]
  (puget/pprint value {:print-handlers @dp-puget/handlers
                       :print-fallback :print}))

(defn pprint-str
  [value]
  (puget/pprint-str value {:print-handlers @dp-puget/handlers
                           :print-fallback :print}))

(defn print-edn
  "Use `print-edn'` to generate an EDN string and check if it can be read by `read-edn`.
  "
  [value]
  (let [string (pprint-str value)]
    (truss/have! read-edn string
                 :data {:msg    "EDN string generated, but failed to read-edn"
                        :string string
                        :value  value})
    string))

(comment
  ;;;
  (pprint-str (atom {:a :b}))
  (pprint-str identity)
  (pprint-str +)
  (pprint-str #time/duration "PT1S")

  (pprint (atom {:a :b}))

  (clojure.pprint/pprint (atom {:a :b}))
  (pr-str (atom {:a :b}))
  (pr-str +)

  ;;
  (print-edn {:a :b})
  (print-edn #time/duration "PT1S")
  ;;;
  )


;;; tagged literals

(defn register-printer
  "Register print handlers for tagged literals across print/pprint implementations.

  Don't forget to configure the tag in `data_readers.clj` or `data_readers.cljc`, then the tagged literals can be used for both reading and printing.

  - `type` the type this handler is for (`java.lang.Class` or JS constructor)
  - `tag` the symbol used as tag, without the `#`
  - `to-edn` a function which takes the object to be printed, and returns plain
    data (vectors, maps, etc)

  Ex:

  ;; see https://github.com/lambdaisland/deja-fu/blob/main/src/lambdaisland/deja_fu.cljs#L407
  (register-print LocalTime 'time/time (comp str format))

  ; see https://github.com/plexus/mad-sounds/blob/2715fe5c26e40f9dd7ad26370b1f237e614fd5ac/src/mad_sounds/sessions/aarschot_2023_12_17_sample_picker.clj#L13
  (register-print clojure.lang.Atom \"atom\" deref)

  (register-print CustomType 'my.ns/CustomType (fn [obj] {:x (.-x obj)}))

  See
  - https://clojure.org/reference/reader#tagged_literals
  - https://github.com/lambdaisland/data-printers?tab=readme-ov-file#usage"
  [type tag to-edn]
  (dp/register-print type tag to-edn)
  (dp/register-pprint type tag to-edn)
  (dp-puget/register-puget type tag to-edn)
  (dp-ddiff2/register-deep-diff2 type tag to-edn)
  (dp-transit/register-write-handler type tag to-edn))

(comment
  ;;;
  ;; before
  (atom {:a :b})
  ;=> #object[clojure.lang.Atom 0x5438122c {:status :ready, :val {:a :b}}]

  ;;
  (register-printer clojure.lang.Atom 'atom deref)

  ;; after
  (atom {:a :b})
  ;=> #atom{:a :b}

  (read-edn "#object[clojure.lang.Atom 0x5438122c {:status :ready, :val {:a :b}}]")

  ;;;
  )

;; see https://metaredux.com/posts/2019/12/05/pimp-my-print-method-prettier-clojure-built-in-types.html
;; ex: (atom {:a :b}) ;=> #atom[{:a :b} 0x6113de1f]
(require '[cider.nrepl.print-method])

;; see https://github.com/henryw374/time-literals?tab=readme-ov-file#reading-and-writing-edn
;; ex: #time/date "2022-01-01"
(time-literals.read-write/print-time-literals-clj!)
