(ns clojure_server.header-parser)

(defn parse-headers [scanner]
  (let [request-line (clojure.string/split (.nextLine scanner) 
                                           #" ")]
    (loop [headers (hash-map :method (first request-line)
                             :path (second request-line)
                             :http-version (last request-line))
           line (.nextLine scanner)]
      (if-not (empty? line) 
        (let [[_, field, value] (re-matches #"(.*):\s+(.*)" line)]
          (recur (assoc headers (keyword field) value)
                 (.nextLine scanner)))
        headers))))
