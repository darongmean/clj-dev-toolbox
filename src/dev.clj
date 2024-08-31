(ns dev
  (:require
   [clojure.repl :as repl]
   [dev.data]
   [dev.http]
   [dev.inspect]
   [dev.snapshot]
   [dev.system]
   [dev.test]
   [sc.api]
   [talltale.core]
   [vinyasa.inject :as inject]))


;;; schema and data tools

(inject/inject 'dev '[[dev.data
                       ;;
                       infer-schema
                       parse
                       parse-json
                       read-json
                       print-json
                       print-json'
                       read-edn
                       print-edn
                       pprint-str
                       pprint]])

;;; snapshot data tools

(inject/inject 'dev '[[dev.snapshot
                       ;;
                       snapshot
                       check-snapshot
                       write-snapshot
                       read-snapshot-n
                       read-snapshot]])

;;; fake data tools

(inject/inject 'dev 'fake- '[[talltale.core
                              ;; see https://github.com/jgrodziski/talltale?tab=readme-ov-file#all-available-generators
                              username
                              username-gen
                              random-password
                              random-password-gen
                              first-name
                              first-name-gen
                              last-name
                              last-name-gen

                              email
                              email-gen
                              url
                              url-gen
                              logo-url
                              logo-url-gen

                              lorem-ipsum
                              lorem-ipsum-gen

                              quality
                              quality-gen]])

;;; test tools

(inject/inject 'dev '[[dev.test
                       ;;
                       exercise
                       exercise-n
                       exercise-qc
                       check
                       check-schema]])

;;; debug tools

;; (sc.api/spy ...) ;; insert a `spy` call in the scope of these locals
;; (sc.api/defsc 7) ;; recreate the scope by def-ing Vars
(inject/inject 'dev '[[sc.api
                       ;;
                       spy
                       defsc]])

;;; state management tools

(inject/inject 'dev '[[dev.system
                       ;;
                       start
                       stop
                       restart]])

;;; http client tools

(inject/inject 'dev '[[dev.http
                       ;;
                       http]])


;;; inspect tools

(inject/inject 'dev '[[dev.inspect
                       ;;
                       *v
                       inspect
                       inspect-table
                       inspect-diff
                       inspect-diff2]])

;;; help

(inject/inject 'dev '[[clojure.repl
                       ;;
                       dir
                       doc]])


(defn tools
  "List all the tools defined in the dev namespace"
  []
  (println "The following tools are defined in the dev namespace:")
  (repl/dir dev))


(println "---------------------------------------------------------")
(println "Loading dev namespace tools...")
(println "---------------------------------------------------------")

(defn help
  "Print help text.

  Ex:
  ```clj

  (dev/help)

  ```
  "
  ([]
   (println "(dev/help)                             ; print help text")
   (println "(dev/tools)                            ; list all the tools defined in the dev namespace")
   (println "(dev/doc ...) ; ex: (dev/doc dev/help) ; print the API documentation of a given Var")
   (println)
   (println "Tagged literals:                       ; see https://github.com/henryw374/time-literals?tab=readme-ov-file#usage")
   (println "ex: #time/date \"2022-01-01\"")
   (println)
   (println "Pretty print:                          ; see https://metaredux.com/posts/2019/12/05/pimp-my-print-method-prettier-clojure-built-in-types.html")
   (println "ex: (dev/pprint (atom {:a :b}))        ;:=> #atom[{:a :b} 0x6cdba772]")
   (println)
   (tools)))

(help)

(println "---------------------------------------------------------")

(comment
  ;;;
  (repl/dir dev)
  (repl/doc dev/infer-schema)

  ;;
  ;;;
  )
