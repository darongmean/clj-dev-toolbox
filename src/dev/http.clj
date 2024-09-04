(ns dev.http
  (:require
    [babashka.fs :as fs]
    [cheshire.core :as cheshire]
    [clojure.string :as string]
    [dev.inspect :as inspect]
    [dev.snapshot :as snapshot]
    [hato.client :as hato]
    [hato.middleware :as hato.middleware]
    [ring.util.http-status :as http-status]))

;;; dynamic vars

(def **req (atom nil))


(defn *req
  "The most recent value of HTTP request."
  []
  (deref **req))


(def **res (atom nil))


(defn *res
  "The most recent value of HTTP response."
  []
  (deref **res))

;;;

(defn http-snapshot [snapshot {:keys [server-name uri request-method]}]
  (update snapshot :snapshot/dir fs/path (str server-name
                                           fs/file-separator
                                           (string/replace uri "/" fs/file-separator)
                                           fs/file-separator
                                           "_"
                                           (name request-method))))


(defn read-http-n
  [n snapshot method url]
  (let [request (assoc (hato.middleware/parse-url url) :request-method method)
        hs      (http-snapshot snapshot request)]
    (snapshot/read-snapshot-n n hs)))


(defn read-http
  [snapshot method url]
  (let [request (assoc (hato.middleware/parse-url url) :request-method method)
        hs      (http-snapshot snapshot request)]
    (snapshot/read-snapshot hs)))


;;; inspect tools

(defn println-json-or-text [s]
  (try
    (-> s
      (cheshire/parse-string)
      (cheshire/generate-string {:pretty true})
      (println))
    (catch Exception _
      (println s))))


(defn version-str [v]
  (cond
    (= :http-1.1 v) "HTTP/1.1"
    (= :http-2 v) "HTTP/2"
    :else (str v)))


(defn status-str [status]
  (let [n (get-in http-status/status [status :name])]
    (if (some? n)
      (str status " " n)
      "N/A")))


(defn capitalize-snake-case [s]
  (->> (string/split (string/lower-case s) #"-")
    (map string/capitalize)
    (string/join "-")))


(defn request-method-str [rm]
  (string/upper-case (name rm)))


(defn println-request [version request]
  (let [{:keys [request-method uri server-name headers body query-string]} request

        bd-or-qs (or body query-string)]
    (println (request-method-str request-method) (or (not-empty uri) "/") (version-str version))
    (println "Host:" server-name)

    (doseq [[k v] (sort-by first headers)]
      (println (-> k (capitalize-snake-case) (str ":")) v))

    (when (some? bd-or-qs)
      (println)
      (println-json-or-text bd-or-qs))))


(defn println-response [version response]
  (let [{:keys [status headers body]} response]
    (println (version-str version) (status-str status))

    (doseq [[k v] (sort-by first headers)
            :when (not= k ":status")]
      (println (-> k (capitalize-snake-case) (str ":")) v))

    (when (some? body)
      (println)
      (println-json-or-text body))))


(defn pprint-http
  ([ring]
   (pprint-http :no-headers ring))

  ([with-headers ring]
   (let [{:keys [request-method] :as request} (:request ring)
         {:keys [status uri version] :as response} (or (:response ring) ring)

         version  (or version (:version request))
         request  (if (= :with-headers with-headers)
                    request
                    (dissoc request :headers))
         response (if (= :with-headers with-headers)
                    response
                    (-> response
                      (dissoc :headers)
                      (assoc-in [:headers "content-type"] (get-in response [:headers "content-type"]))))]
     (println)
     (println (status-str status) "-" (request-method-str request-method) "-" uri)
     (println)
     (println-request version request)
     (println)
     (println-response version response)
     (println)))

  ([snapshot method url]
   (pprint-http (read-http snapshot method url)))

  ([with-headers snapshot method url]
   (pprint-http with-headers (read-http snapshot method url))))


(defn inspect-http
  ([value]
   (inspect/inspect :hiccup [:portal.viewer/text (with-out-str (pprint-http value))])
   value)

  ([with-headers value]
   (inspect/inspect :hiccup [:portal.viewer/text (with-out-str (pprint-http with-headers value))])
   value)

  ([snapshot method url]
   (let [value (read-http snapshot method url)]
     (inspect/inspect :hiccup [:portal.viewer/text (with-out-str (pprint-http value))])
     value))

  ([with-headers snapshot method url]
   (let [value (read-http snapshot method url)]
     (inspect/inspect :hiccup [:portal.viewer/text (with-out-str (pprint-http with-headers value))])
     value)))


(comment
  ;;;
  (do
    (def response
      {:request-time        474,
       :request             {:user-info      nil,
                             :headers        {"accept-encoding" "gzip, deflate"},
                             :server-port    nil,
                             :url            "https://darongmean.com",
                             :http-request   'jdk.internal.net.http.HttpRequestImpl,
                             :uri            "",
                             :server-name    "darongmean.com",
                             :query-string   nil,
                             :scheme         :https,
                             :request-method :get},
       :http-client         'jdk.internal.net.http.HttpClientFacade,
       :headers             {"x-cache"                     "MISS",
                             "x-timer"                     "S1725208601.367644,VS0,VE99",
                             "content-encoding"            "gzip",
                             "server"                      "cloudflare",
                             "x-proxy-cache"               "MISS",
                             "age"                         "0",
                             "via"                         "1.1 varnish",
                             "content-type"                "text/html; charset=utf-8",
                             "access-control-allow-origin" "*",
                             "alt-svc"                     "h3=\":443\"; ma=86400",
                             "nel"                         "{\"success_fraction\":0,\"report_to\":\"cf-nel\",\"max_age\":604800}",
                             "expires"                     "Sun, 01 Sep 2024 16:46:41 GMT",
                             "x-github-request-id"         "E36C:35AB35:10D4CA7:1139A57:66D49819",
                             "cf-cache-status"             "DYNAMIC",
                             "cf-ray"                      "8bc6ae3e0a116dea-CPH",
                             "x-cache-hits"                "0",
                             ":status"                     "200",
                             "date"                        "Sun, 01 Sep 2024 16:36:41 GMT",
                             "vary"                        "Accept-Encoding",
                             "last-modified"               "Sat, 03 Dec 2022 07:09:03 GMT",
                             "report-to"                   "{\"endpoints\":[{\"url\":\"https:\\/\\/a.nel.cloudflare.com\\/report\\/v4?s=OzOkIqMih5xHv%2FF05D4dKybcNUbBHPBApW1lnIlJB%2F27pXICq3If8zE1DDr2pwBPck%2FaEhxQWeYYhBFCf3g8x0Lg9EYKce1qtPdYvsPSsD9IYdY4iI18XiLmkheTq%2Ftsiw%3D%3D\"}],\"group\":\"cf-nel\",\"max_age\":604800}",
                             "cache-control"               "max-age=600",
                             "x-served-by"                 "cache-ams2100122-AMS",
                             "x-fastly-request-id"         "f44d514b7e40a0e876764373021d56ee77ab96bf"},
       :status              200,
       :content-type        :text/html,
       :uri                 "https://darongmean.com",
       :content-type-params {:charset "utf-8"},
       :version             :http-2,
       :body                "body"}))

  ;;
  (pprint-http response)
  (pprint-http :with-headers response)

  ;;
  (http :get "https://darongmean.com")

  (pprint-http *1)
  (inspect-http *1)

  ;;;
  )


(defn to-edn [ring]
  (-> ring
    (dissoc :http-client)
    (assoc :request (dissoc (:request ring) :http-request))))

;;; Http client tools

(defn make-request
  "Make a Http request.
  See https://github.com/gnarroway/hato?tab=readme-ov-file#making-queries

  Ex:

  (make-request :get \"https://darongmean.com\")
  (make-request :post \"https://darongmean.com\")

  "
  ([req]
   (let [resp (hato/request req)]

     (reset! **req req)
     (reset! **res resp)

     resp))

  ([method url & [opts]]
   (let [resp (#'hato/configure-and-execute
                method
                url
                (merge
                  {:version          :http-1.1
                   :content-type     :json
                   :throw-exceptions false}
                  opts))]

     (reset! **req (merge opts {:request-method method :url url}))
     (reset! **res resp)

     resp)))


(defn http
  "Make a Http request and save the response to snapshot.
  See https://github.com/gnarroway/hato?tab=readme-ov-file#making-queries

  Ex:

  (http (dev/snapshot \"/tmp/testdata\") :get \"https://darongmean.com\")

  (http (dev/snapshot \"/tmp/testdata\") :post \"https://darongmean.com\"
    {:form-params  {:a \"b\" :c :d}})

  (http (dev/snapshot \"/tmp/testdata\") :post \"https://darongmean.com\"
    {:content-type :json
     :form-params  {:a \"b\" :c :d}})

  (http (dev/snapshot \"/tmp/testdata\") :post \"https://darongmean.com\"
    {:content-type :x-www-form-urlencoded
     :form-params  {:a \"b\" :c :d}})

  (http (dev/snapshot \"/tmp/testdata\") :post \"https://darongmean.com\"
    {:content-type \"text/plain\"
     :form-params  {:a \"b\" :c :d}})

  "
  ([snapshot method url]
   (http snapshot method url {}))

  ([snapshot method url opts]
   (let [{:keys [request] :as data} (to-edn (make-request method url opts))
         s (http-snapshot snapshot request)]

     (snapshot/write-snapshot s data)
     (pprint-http (snapshot/read-snapshot s)))))

(comment
  ;;; check snapshot directory
  (http-snapshot (snapshot/snapshot "/tmp/testdata") {:server-name    "localhost"
                                                      :uri            "/test/abc"
                                                      :request-method :get})
  (http-snapshot (snapshot/snapshot "/tmp/testdata") {:server-name    "localhost"
                                                      :uri            ""
                                                      :request-method :get})
  (http-snapshot (snapshot/snapshot "/tmp/testdata") {:server-name    "localhost"
                                                      :uri            "/"
                                                      :request-method :get})

  ;; make requests with snapshot
  (http (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com")
  (http (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com/abc?a=b")

  (http (snapshot/snapshot "/tmp/testdata")
    :post "https://webhook-test.com/f0540553e2e7ce1f2c0277c4ebcf6810"
    {:form-params {:a "b" :c :d}})

  ;; check most recent values
  (*req)
  (*res)

  ;; read snapshot
  (read-http (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com/abc")
  (read-http-n 2 (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com")

  ;; pprint snapshot
  (pprint-http (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com")
  (pprint-http :with-headers (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com")

  ;; inspect snapshot
  (inspect-http (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com")
  (inspect-http :with-headers (snapshot/snapshot "/tmp/testdata") :get "https://darongmean.com")

  ;;;
  )
