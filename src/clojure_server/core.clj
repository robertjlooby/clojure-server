(ns clojure_server.core
  (:import java.net.ServerSocket))

(defn create-server-socket 
  ([port] (java.net.ServerSocket. port))
  ([port address] (java.net.ServerSocket. port 0 address)))

(defn listen [server-socket]
  (.accept server-socket))

(defn echo-server [port inet-address]
  (loop [server-socket (create-server-socket port inet-address)
         socket (listen server-socket)]
    (let [scanner (java.util.Scanner. (.getInputStream socket))
          o-stream (java.io.PrintWriter. (.getOutputStream socket) true)]
      (.println o-stream (second (clojure.string/split (.nextLine scanner) #"\s+/?")))
      (.close socket))
    (recur server-socket (listen server-socket))))

