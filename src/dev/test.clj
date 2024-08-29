(ns dev.test
  (:require
   [clojure.test :as t]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.properties :as prop']
   [dev.data :as data]
   [malli.core :as m]
   [malli.generator :as mg]
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
   (gen/sample (mg/generator schema) n))
  ([n f schema]
   (let [d  (atom [])
         qc (tc/quick-check n
              (prop'/for-all [input (mg/generator schema)]
                (let [result (f input)]
                  (swap! d conj result)
                  result)))]
     (if (:pass? qc)
       (deref d)
       (format-quick-check qc))))
  ([n f schema1 schema2]
   (let [d  (atom [])
         qc (tc/quick-check n
              (prop'/for-all [input01 (mg/generator schema1)
                              input02 (mg/generator schema2)]
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
   (gen/sample (mg/generator schema)))
  ([f schema]
   (let [qc (tc/quick-check 100
              (prop'/for-all [input (mg/generator schema)]
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
    (m/coerce schema value)
    (catch Exception ex
      (let [error-msg (data/ex-message ex)]
        (t/is (= "" error-msg) (str value))))))

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
