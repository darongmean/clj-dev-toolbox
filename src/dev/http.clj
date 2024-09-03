(ns dev.http
  (:require
    [cheshire.core :as cheshire]
    [clojure.string :as string]
    [dev.inspect :as inspect]
    [hato.client :as hato]))


(defn http
  "See https://github.com/gnarroway/hato?tab=readme-ov-file#making-queries

  Ex:
  ```clj

  (http :get \"https://darongmean.com\")
  (http :post \"https://darongmean.com\")

  ```
  "
  [method url & [opts respond raise]]
  (#'hato/configure-and-execute method url opts respond raise))


;;; inspect tools

(defn println-json-or-text [s]
  (try
    (-> s
      (cheshire/parse-string)
      (cheshire/generate-string {:pretty true})
      (println))
    (catch Exception _
      (println s))))


(defn http-version [v]
  (cond
    (= :http-1.1 v) "HTTP/1.1"
    (= :http-2 v) "HTTP/2"
    :else (str v)))


(defn capitalize-snake-case [s]
  (->> (string/split (string/lower-case s) #"-")
    (map string/capitalize)
    (string/join "-")))


(defn request-method-str [rm]
  (string/upper-case (name rm)))


(defn println-request [version request]
  (let [{:keys [request-method uri server-name headers body query-string]} request

        bd-or-qs (or body query-string)]
    (println (request-method-str request-method) (or (not-empty uri) "/") version)
    (println "Host:" server-name)

    (doseq [[k v] (sort-by first headers)]
      (println (-> k (capitalize-snake-case) (str ":")) v))

    (when (some? bd-or-qs)
      (println)
      (println-json-or-text bd-or-qs))))


(defn println-response [version response]
  (let [{:keys [status headers body]} response]
    (println version status)

    (doseq [[k v] (sort-by first headers)
            :when (not= k ":status")]
      (println (-> k (capitalize-snake-case) (str ":")) v))

    (when (some? body)
      (println)
      (println-json-or-text body))))


(defn print-http
  ([ring]
   (print-http :no-headers ring))
  ([with-headers ring]
   (let [{:keys [request-method url version] :as request} (:request ring)
         {:keys [status] :as response} (or (:response ring) ring)

         version  (or version (:version response) (:version ring))
         request  (if (= :with-headers with-headers)
                    request
                    (dissoc request :headers))
         response (if (= :with-headers with-headers)
                    response
                    (-> response
                      (dissoc :headers)
                      (assoc-in [:headers "content-type"] (get-in response [:headers "content-type"]))))]
     (println)
     (println status "-" (request-method-str request-method) "-" url)
     (println)
     (println-request (http-version version) request)
     (println)
     (println-response (http-version version) response)
     (println))))


(defn inspect-http
  ([value]
   (inspect/inspect :hiccup [:portal.viewer/text (with-out-str (print-http value))])
   value)
  ([with-headers value]
   (inspect/inspect :hiccup [:portal.viewer/text (with-out-str (print-http with-headers value))])
   value))


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
  (print-http response)
  (print-http :with-headers response)

  ;;
  (http :get "https://darongmean.com")

  (print-http *1)
  (inspect-http *1)

  ;;;
  )
