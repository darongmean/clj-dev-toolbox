(ns dev.http.har)


(def Creator
  [:map
   {:closed true}
   [:name string?]
   [:version string?]
   [:comment {:optional true} string?]])

(def Browser
  [:map
   {:closed true}
   [:name string?]
   [:version string?]
   [:comment {:optional true} string?]])

(def Page
  [:schema
   {:registry
    {::page
     [:map
      {:closed true}
      [:startedDateTime inst?]
      [:id string?]
      [:title string?]
      [:pageTimings [:ref ::page-timings]]
      [:comment {:optional true} string?]]

     ::page-timings
     [:map
      {:closed true}
      [:onContentLoad {:optional true} [:or number? [:= -1]]]
      [:onLoad {:optional true} [:or number? [:= -1]]]
      [:comment {:optional true} string?]]}}
   ::page])

(def Entry
  [:schema
   {:registry
    {::entry
     [:map
      {:closed true}
      [:pageref {:optional true} string?]
      [:startedDateTime inst?]
      [:time [:and number? [:>= 0]]]
      [:request [:ref ::request]]
      [:response [:ref ::response]]
      [:cache [:ref ::cache]]
      [:timings [:ref ::timings]]
      [:serverIPAddress {:optional true} string?]
      [:connection {:optional true} string?]
      [:comment {:optional true} string?]]

     ::request
     [:map
      {:closed true}
      [:method string?]
      [:url string?]
      [:httpVersion string?]
      [:cookies [:vector [:ref ::cookie]]]
      [:headers [:vector [:ref ::record]]]
      [:queryString [:vector [:ref ::record]]]
      [:postData {:optional true} [:ref ::post-data]]
      [:headersSize int?]
      [:bodySize int?]
      [:comment {:optional true} string?]]

     ::response
     [:map
      {:closed true}
      [:status int?]
      [:statusText string?]
      [:httpVersion string?]
      [:cookies [:vector [:ref ::cookie]]]
      [:headers [:vector [:ref ::record]]]
      [:content [:ref ::content]]
      [:redirectURL string?]
      [:headersSize int?]
      [:bodySize int?]
      [:comment {:optional true} string?]]

     ::cookie
     [:map
      {:closed true}
      [:name string?]
      [:value string?]
      [:path {:optional true} string?]
      [:domain {:optional true} string?]
      [:expires {:optional true} string?]
      [:httpOnly {:optional true} boolean?]
      [:secure {:optional true} boolean?]
      [:comment {:optional true} string?]]

     ::record
     [:map
      {:closed true}
      [:name string?]
      [:value string?]
      [:comment {:optional true} string?]]

     ::post-data
     [:map
      {:closed true}
      [:mimeType string?]
      [:text {:optional true} string?]
      [:params
       {:optional true}
       [:vector
        [:map
         {:closed true}
         [:name string?]
         [:value {:optional true} string?]
         [:fileName {:optional true} string?]
         [:contentType {:optional true} string?]
         [:comment {:optional true} string?]]]]
      [:comment {:optional true} string?]]

     ::content
     [:map
      {:closed true}
      [:size int?]
      [:compression {:optional true} int?]
      [:mimeType string?]
      [:text {:optional true} string?]
      [:encoding {:optional true} string?]
      [:comment {:optional true} string?]]

     ::cache
     [:map
      {:closed true}
      [:beforeRequest {:optional true} [:ref ::cache-entry]]
      [:afterRequest {:optional true} [:ref ::cache-entry]]
      [:comment {:optional true} string?]]

     ::cache-entry
     [:map
      {:closed true}
      [:expires {:optional true} string?]
      [:lastAccess string?]
      [:eTag string?]
      [:hitCount int?]
      [:comment {:optional true} string?]]

     ::timings
     [:map
      {:closed true}
      [:dns {:optional true} [:or number? [:= -1]]]
      [:connect {:optional true} [:or number? [:= -1]]]
      [:blocked {:optional true} [:or number? [:= -1]]]
      [:send [:or number? [:= -1]]]
      [:wait [:or number? [:= -1]]]
      [:receive [:or number? [:= -1]]]
      [:ssl {:optional true} [:or number? [:= -1]]]
      [:comment {:optional true} string?]]}}
   ::entry])


(def HAR
  [:map
   {:closed true}
   [:log
    [:map
     {:closed true}
     [:version {:optional true} string?]
     [:creator {:optional true} #'Creator]
     [:browser {:optional true} #'Browser]
     [:pages {:optional true} [:vector #'Page]]
     [:entries [:vector #'Entry]]
     [:comment {:optional true} string?]]]])

(comment
  ;;
  (do
    (def short-har (dev/read-json (slurp "./dev-resources/har-examples/data/short.har"))))

  (require 'babashka.fs)

  ;; list all files in har-examples
  (babashka.fs/list-dir "./dev-resources/har-examples/data" "*.har")
  (def f1 (first *1))
  (slurp (babashka.fs/file f1))

  (def har-files
    (->>
      (babashka.fs/list-dir "./dev-resources/har-examples/data" "*.har")
      (map babashka.fs/file)
      (map slurp)
      (map dev/read-json)
      (doall)))

  (dev/infer-schema har-files)

  (dev/exercise-n 2 HAR)

  (dev/check-schema HAR short-har)
  ;;
  )


;;; har sanitizer
;;; https://github.com/cloudflare/har-sanitizer/blob/main/src/lib/har_sanitize.tsx
;;; https://blog.cloudflare.com/introducing-har-sanitizer-secure-har-sharing/
;;; https://har-sanitizer.pages.dev/

;;; har analyzer
;;; http://www.softwareishard.com/har/viewer/
;;; https://toolbox.googleapps.com/apps/har_analyzer/
;;; https://github.com/janodvarko/harviewer/tree/master

;;; spec and schemas
;;; http://www.softwareishard.com/blog/har-12-spec/
;;; https://github.com/readmeio/har-examples/tree/main
;;; https://github.com/delvelabs/marshmallow-har/tree/master
;;; https://github.com/deviantintegral/har?tab=readme-ov-file
