(ns clojure_server.header-parser)

(defn parse-headers [in-seq]
  (let [request-line (clojure.string/split (first in-seq) 
                                           #" ")
        [headers rest-lines] 
                (loop [heads (hash-map :method (first request-line)
                              :path (second request-line)
                              :http-version (last request-line))
                       lines (rest in-seq)]
                  (if-not (empty? (first lines)) 
                   (let [[_, field, value] 
                          (re-matches #"(.*):\s+(.*)" (first lines))]
                     (recur (assoc heads (keyword field) value)
                            (rest lines)))
                     [heads (rest lines)]))]
    (if (and (:Content-Length headers)
             (> (read-string (:Content-Length headers)) 0))
      (loop [body []
             lines rest-lines]
        (if-not (empty? (first lines))
          (recur (conj body (first lines)) (rest lines))
          (assoc headers :body body)))
      headers)))
