(ns clojure_server.response-builder)

(defn build-response [router-response]
  (lazy-cat
    (list (str "HTTP/1.1 " (second router-response) " OK") "")
    (first router-response)
    '("")))
