(ns clojure_server.response-builder)

(def codes {200 "OK"
            206 "Partial Content"
            301 "Moved Permanently"
            401 "Unauthorized"
            404 "Not Found"
            405 "Method Not Allowed"
           })

(defn status-code-converter [status-code]
  (let [response (get codes status-code)]
    (if response (str status-code " " response))))

(defn build-response [router-response]
  (lazy-cat
    (list 
      (str "HTTP/1.1 " 
           (status-code-converter (second router-response))))
    (map #(str (name (first %)) ": " (second %)) 
         (:headers (first router-response)))
    '("")))
