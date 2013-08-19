(ns clojure_server.request-parser)

(defn socket-reader [socket]
  (clojure.java.io/reader socket))

(defn read-until-emptyline [reader]
    (take-while #(seq %)
      (repeatedly #(.readLine reader))))

(defn read-n-bytes [reader num-bytes]
  (let [carr   (char-array num-bytes)
        n-read (.read reader carr 0 num-bytes)
        trimmed-carr (if (= n-read num-bytes) carr
                       (char-array n-read carr))]
    (line-seq (java.io.BufferedReader.
                (java.io.StringReader.
                  (apply str (seq trimmed-carr)))))))

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
