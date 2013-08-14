(ns clojure_server.response-builder)

(defn status-code-converter [status-code]
  (let [codes {200 "OK"
               404 "Not Found"
              }
        response (get codes status-code)]
    (if response (str status-code " " response))))

(defn build-response [router-response]
  (lazy-cat
    (list 
      (str "HTTP/1.1 " 
           (status-code-converter (second router-response))))
    '("")
    (first router-response)
    '("")))
