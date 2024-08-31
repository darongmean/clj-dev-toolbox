(ns dev.testing
  (:require
    [clojure.test :as clj.test]
    [clojure.test.check :as test.check]
    [clojure.test.check.generators :as test.check.generators]
    [com.gfredericks.test.chuck.properties :as properties]
    [dev.data :as data]
    [malli.core :as malli]
    [malli.generator :as malli.generator]
    [matcho.core :as matcho]))


(defn format-quick-check [qc]
  (-> qc
    (update-in [:result] #(or (data/ex-message %) (ex-message %) %))
    (update-in [:result-data :clojure.test.check.properties/error] #(or (data/ex-message %) (ex-message %) %))
    (update-in [:shrunk :result] #(or (data/ex-message %) (ex-message %) %))
    (update-in [:shrunk :result-data :clojure.test.check.properties/error] #(or (data/ex-message %) (ex-message %) %))))


(defn exercise-n
  "Ex:

  (exercise-n 10 [:map [:a :string]])
  (exercise-n 10 identity [:map [:a :string]])

  ;; errors
  (exercise-n 10 boolean? :string)

  See
  - https://clojuredocs.org/clojure.spec.alpha/exercise
  - https://youtu.be/Qx0-pViyIDU?list=PLqlIeRk5-BL7wc6OWdXmGcAGc4XId82-1&t=1014
  "
  ([n schema]
   (test.check.generators/sample (malli.generator/generator schema) n))
  ([n f schema]
   (let [d  (atom [])
         qc (test.check/quick-check n
              (properties/for-all [input (malli.generator/generator schema)]
                (let [result (f input)]
                  (swap! d conj result)
                  result)))]
     (if (:pass? qc)
       (deref d)
       (format-quick-check qc))))
  ([n f schema1 schema2]
   (let [d  (atom [])
         qc (test.check/quick-check n
              (properties/for-all [input01 (malli.generator/generator schema1)
                                   input02 (malli.generator/generator schema2)]
                (swap! d conj (f input01 input02))))]
     (if (:pass? qc)
       (deref d)
       (format-quick-check qc)))))

(comment
  (exercise-n 10 [:map [:a :string]])
  (exercise-n 10 identity [:map [:a :string]])

  ;; errors
  (exercise-n 10 boolean? :string)
  ;;;
  )


(defn exercise-qc
  "Same as exercise, but always returning quick-check result.
  Ex:

  (exercise-qc [:map [:a :string]])
  (exercise-qc identity [:map [:a :string]])

  ;; errors
  (exercise-qc boolean? :string)

  See
  - https://clojuredocs.org/clojure.spec.alpha/exercise
  - https://youtu.be/Qx0-pViyIDU?list=PLqlIeRk5-BL7wc6OWdXmGcAGc4XId82-1&t=1014
  "
  ([schema]
   (test.check.generators/sample (malli.generator/generator schema)))
  ([f schema]
   (let [qc (test.check/quick-check 100
              (properties/for-all [input (malli.generator/generator schema)]
                (f input)))]
     (format-quick-check qc))))

(comment
  (exercise-qc [:map [:a :string]])
  (exercise-qc identity [:map [:a :string]])

  ;; errors
  (exercise-qc boolean? :string)
  ;;;
  )


(defn exercise
  "Ex:

  (exercise [:map [:a :string]])
  (exercise identity [:map [:a :string]])

  ;; errors
  (exercise boolean? :string)

  See
  - https://clojuredocs.org/clojure.spec.alpha/exercise
  - https://youtu.be/Qx0-pViyIDU?list=PLqlIeRk5-BL7wc6OWdXmGcAGc4XId82-1&t=1014
  "
  ([schema]
   (exercise-n 10 schema))
  ([f schema]
   (exercise-n 10 f schema))
  ([f schema1 schema2]
   (exercise-n 10 f schema1 schema2)))

(comment
  (exercise [:map [:a :string]])
  (exercise identity [:map [:a :string]])

  ;; errors
  (exercise boolean? :string)
  ;;;
  )


(defn check-schema
  "Check `value` against the `schema` using the Malli library.

  Ex:

  (check-schema [:map] {:b 2})

  ;; errors
  (check-schema [:map] 123)
  "
  [schema value]
  (try
    (malli/coerce schema value)
    (catch Exception ex
      (let [error-msg (data/ex-message ex)]
        (clj.test/is (= "" error-msg) (str value))))))

(comment
  (check-schema [:map] {:b 2})

  ;; errors
  (check-schema [:map] 123)
  ;;;
  )


(defn check
  "Check `value` against the `pattern` using Matcho library.

  Ex:

  (check {:a 1} {:a 1 :b 2})

  ;; errors
  (check {:a 1} {:b 2})
  "
  [pattern value]
  (if (matcho/valid? pattern value)
    value
    (matcho/assert pattern value)))

(comment
  (check {:a 1} {:a 1 :b 2})

  ;; errors
  (check {:a 1} {:b 2})
  ;;;
  )


;; TODO: a tool to investigate generator statistics report
;; see
;; - https://www.cse.chalmers.se/~rjmh/QuickCheck/manual.html collect/classify functions
;; - https://hypothesis.readthedocs.io/en/latest/details.html#test-statistics
;; - https://medium.com/nerd-for-tech/multiplying-the-quality-of-your-unit-tests-with-property-based-tests-d3c2cb2d5601
