(ns dev
  (:require
   [clojure.repl :as repl]
   [dev.data]
   [dev.http]
   [dev.inspect]
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
                       decode-json
                       encode-json]])

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
   (println "(dev/dir ...) ; ex: (dev/dir dev)      ; list the names of all the Vars defined a given namespace")
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
