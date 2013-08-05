(ns clojure_server.header-parser)

(defn parse-headers [scanner]
  (let [headers (transient {})
        request-line (clojure.string/split (.nextLine scanner) 
                                           #" ")]
    (assoc! headers :method (first request-line))
    (assoc! headers :path (second request-line))
    (assoc! headers :http-version (last request-line))
    (loop [line (.nextLine scanner)]
      (if-not (empty? line) 
        (let [[_, field, value] (re-matches #"(.*):\s+(.*)" line)]
          (assoc! headers (keyword field) value)
          (recur (.nextLine scanner)))))
    (persistent! headers)))
