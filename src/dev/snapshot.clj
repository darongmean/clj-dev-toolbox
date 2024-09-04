(ns dev.snapshot
  (:refer-clojure :exclude [replace])
  (:require
    [babashka.fs :as fs]
    [clojure.set :as set]
    [dev.data :as data]
    [dev.testing :as testing]))

;;; dynamic vars

(def **snapshot (atom nil))


(defn *snapshot
  "The most recent value written to a snapshot."
  []
  (deref **snapshot))

;;;

(defn ns-dir [n]
  (let [pattern (str "regex:.*" (ns-name n) "\\.clj")]
    (->> (fs/match "." pattern {:recursive true})
      (map fs/parent)
      (first))))


(defn parse-dir [{:keys [dir caller-ns] :as _opts}]
  (let [path (if (fs/relative? dir)
               (fs/path (or (ns-dir caller-ns)
                          (throw (ex-info (str "could not find directory of the namespace: " (ns-name caller-ns))
                                   {:caller-ns caller-ns})))
                 dir)
               (fs/path (str "." dir)))]

    {:snapshot/dir (fs/absolutize path)}))

(defn parse-scrubber [{:keys [with-scrubber] :as _opts}]
  {:snapshot/replace with-scrubber
   :snapshot/restore (set/map-invert with-scrubber)})

(defn parse-opts [opts]
  (merge (parse-dir opts)
    (parse-scrubber opts)))


(defmacro snapshot
  ([dir & {:as opts}]
   `(parse-opts (merge {:dir ~dir :caller-ns ~*ns*} ~opts))))


(defn -write-snapshot
  [{:snapshot/keys [dir replace]} value & {:as options}]
  (let [dir-exists? (fs/exists? dir)

        file-name   (str (System/currentTimeMillis) ".edn")
        f           (fs/file dir file-name)

        content     (data/print-edn value
                      :replace (merge replace (:replace options)))]

    {:create-dirs?     (not dir-exists?)
     :create-dirs/args {:path dir}
     :spit/args        {:f       f
                        :content content}}))

(defn write-snapshot
  [snapshot value & {:as options}]
  (let [plan (-write-snapshot snapshot value options)

        {:keys [create-dirs?]} plan
        {:keys [path]} (:create-dirs/args plan)
        {:keys [f content]} (:spit/args plan)]

    (when create-dirs?
      (fs/create-dirs path))

    (spit f content)
    (reset! **snapshot value)

    nil))

(comment
  ;;;

  (parse-opts {:dir "./testdata/snapshot" :caller-ns 'dev.snapshot})
  ;; relative path
  (snapshot "./testdata/snapshot")
  (snapshot "testdata/snapshot")
  ;; absolute path
  (snapshot "/testdata/snapshot")

  ;; write
  (-write-snapshot (snapshot "./testdata/snapshot")
    {:a :b})
  (write-snapshot (snapshot "./testdata/snapshot")
    {:a :b})
  (write-snapshot (snapshot "./testdata/snapshot")
    {:b :c})

  ;; scrubbers
  (parse-scrubber {:with-scrubber {"b" "[b]"}})
  (snapshot "./testdata/snapshot" :with-scrubber {"b" "[scrubbed_b]"})

  (-write-snapshot (snapshot "./testdata/snapshot" :with-scrubber {"b" "[b]"})
    {:a "b"})
  (-write-snapshot (snapshot "./testdata/snapshot")
    {:a "b"}
    :replace {"b" "[b]"})
  (write-snapshot (snapshot "./testdata/snapshot")
    {:a "b"}
    :replace {"b" "[b]"})
  ;;;
  )


(defn -read-snapshot-n
  [n {:snapshot/keys [dir]}]
  (let [files-by-desc (some->> (fs/list-dir dir "*.edn")
                        (filter fs/regular-file?)
                        (sort-by fs/file-name #(compare %2 %1))
                        (map fs/file)
                        (take n))]

    (for [f files-by-desc]
      {:slurp/args {:f f}})))

(defn read-snapshot-n
  ([snapshot]
   (read-snapshot-n 2 snapshot))
  ([n snapshot & {:as options}]
   (let [restore (merge (:snapshot/restore snapshot)
                   (:restore options))]

     (when (fs/exists? (:snapshot/dir snapshot))
       (->> (-read-snapshot-n n snapshot)
         (mapv #(slurp (get-in % [:slurp/args :f])))
         (mapv #(data/read-edn % :replace restore)))))))

(defn read-snapshot
  [snapshot & {:as options}]
  (first (read-snapshot-n 1 snapshot options)))

(comment
  ;;;
  (-read-snapshot-n 3 (snapshot "./testdata/snapshot"))
  (read-snapshot-n 3 (snapshot "./testdata/snapshot"))
  (read-snapshot (snapshot "./testdata/snapshot"))

  ;; scrubbers
  (read-snapshot (snapshot "./testdata/snapshot" :with-scrubber {"b" "[b]"}))
  (read-snapshot (snapshot "./testdata/snapshot")
    ;; note that :restore is a reverse map of :with-scrubber
    :restore {"[b]" "b"})
  ;;;
  )


(defn check-snapshot
  [snapshot value & {:as options}]
  (let [saved          (read-snapshot (select-keys snapshot [:snapshot/dir]))                                           ; ignore :restore and scrubbers

        plan           (-write-snapshot snapshot value options)
        would-be-saved (data/read-edn (get-in plan [:spit/args :content]))]

    (testing/check saved would-be-saved)))

(comment
  ;;;
  ;; should work
  (check-snapshot (snapshot "./testdata/snapshot" :with-scrubber {"b" "[b]"})
    {:a "b"})
  ;; should fail
  (check-snapshot (snapshot "./testdata/snapshot")
    {:a :b})

  ;; :replace option should work as expected
  (check-snapshot (snapshot "./testdata/snapshot" :with-scrubber {":b" ":cde"})
    {:a :b})
  (check-snapshot (snapshot "./testdata/snapshot")
    {:a :b}
    :replace {":b" ":cde"})

  ;; :restore option is ignored
  (check-snapshot (snapshot "./testdata/snapshot")
    {:a :b}
    :restore {"[b]" "b"})
  ;;;
  )
