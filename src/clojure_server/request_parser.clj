(ns clojure_server.request-parser)

(defn parse-headers [in-seq]
  (let [request-line (clojure.string/split (first in-seq) 
                                           #" ")]
    (loop [headers (hash-map :method (first request-line)
                             :path (second request-line)
                             :http-version (last request-line))
           lines (rest in-seq)]
      (if-not (empty? (first lines)) 
        (let [[_, field, value] (re-matches #"(.*):\s+(.*)" 
                                            (first lines))]
          (recur (assoc headers (keyword field) value)
                 (rest lines)))
        headers))))
