(ns clojure_server.response-builder)

(defn status-code-converter [status-code]
  (let [codes {200 "OK"
               301 "Moved Permanently"
               404 "Not Found"
              }
        response (get codes status-code)]
    (if response (str status-code " " response))))

(defn build-response [router-response]
  (lazy-cat
    (list 
      (str "HTTP/1.1 " 
           (status-code-converter (second router-response))))
    (map #(str (name (first %)) ": " (second %)) 
         (:headers (first router-response)))
    '("")
    (:content (first router-response))
    '("")))
