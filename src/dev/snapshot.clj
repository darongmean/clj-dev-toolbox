(ns dev.snapshot
  (:require
   [babashka.fs :as fs]
   [dev.data :as data]))


(defn ns-dir [n]
  (let [pattern (str "regex:.*" (ns-name n) "\\.clj")]
    (->> (fs/match "." pattern {:recursive true})
         (map fs/parent)
         (first))))


(defn parse-opts [{:keys [dir caller-ns] :as _opts}]
  (let [path (if (fs/relative? dir)
               (fs/path (or (ns-dir caller-ns)
                            (throw (ex-info (str "could not find directory of the namespace: " (ns-name caller-ns))
                                            {:caller-ns caller-ns})))
                        dir)
               (fs/path (str "." dir)))]

    {:dir (fs/absolutize path)}))


(defmacro snapshot
  ([dir & {:as opts}]
   `(parse-opts (merge ~opts {:dir ~dir :caller-ns ~*ns*}))))


(defn -write-snapshot
  [{:keys [dir] :as _snapshot} value]
  (let [dir-exists? (fs/exists? dir)

        file-name   (str (System/currentTimeMillis) ".edn")
        f           (fs/file dir file-name)

        content     (data/print-edn value)]

    {:create-dirs-args (when-not dir-exists? {:path dir})
     :spit-args        {:f       f
                        :content content}}))

(defn write-snapshot
  [snapshot value]
  (let [plan (-write-snapshot snapshot value)

        {:keys [path]} (:create-dirs-args plan)
        {:keys [f content]} (:spit-args plan)]

    (when path
      (fs/create-dirs path))

    (spit f content)))

(comment
  ;;;

  (parse-opts {:dir "./testdata/snapshot" :caller-ns 'dev.snapshot})
  ;; relative path
  (snapshot "./testdata/snapshot")
  ;; absolute path
  (snapshot "/testdata/snapshot")

  ;; write
  (-write-snapshot (snapshot "./testdata/snapshot")
                   {:a :b})
  (write-snapshot (snapshot "./testdata/snapshot")
                  {:a :b})
  (write-snapshot (snapshot "./testdata/snapshot")
                  {:b :c})
  ;;;
  )


(defn -read-snapshot-n
  [n {:keys [dir] :as _snapshot}]
  (let [files-by-desc (some->> (fs/list-dir dir "*.edn")
                               (filter fs/regular-file?)
                               (sort-by fs/file-name #(compare %2 %1))
                               (map fs/file)
                               (take n))]

    (for [f files-by-desc]
      {:slurp-args {:f f}})))

(defn read-snapshot-n
  ([snapshot]
   (read-snapshot-n 2 snapshot))
  ([n snapshot]
   (when (fs/exists? (:dir snapshot))
     (->> (-read-snapshot-n n snapshot)
          (mapv #(slurp (get-in % [:slurp-args :f])))
          (mapv data/read-edn)))))

(defn read-snapshot
  [snapshot]
  (first (read-snapshot-n 1 snapshot)))

(comment
  ;;;
  (-read-snapshot-n 3 (snapshot "./testdata/snapshot"))
  (read-snapshot-n 3 (snapshot "./testdata/snapshot"))
  (read-snapshot (snapshot "./testdata/snapshot"))
  ;;;
  )
