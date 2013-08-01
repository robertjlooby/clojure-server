(ns clojure_server.core
  (:import java.net.ServerSocket)
  (:import java.io.PrintWriter))

(defn server-socket [port]
  (java.net.ServerSocket. port))
;(defn server [] 
;  (let [listener (ServerSocket. 3000)]
;    (try
;      (loop [socket (.accept listener)]
;        (try
;          (let [pw (PrintWriter. (.getOutputStream socket) true)]
;            (.println pw "hello"))
;          (finally (.close socket)))
;        (recur (.accept listener)))
;      (finally (.close listener)))))
;(server)
