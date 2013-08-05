(ns clojure_server.core
  (:require [clojure_server.header-parser :refer :all]
            [clojure_server.response-builder :refer :all])
  (:import java.net.ServerSocket))

(defn create-server-socket 
  ([port] (java.net.ServerSocket. port))
  ([port address] (java.net.ServerSocket. port 0 address)))

(defn listen [server-socket]
  (.accept server-socket))

(defn echo-server [server-socket]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [scanner (java.util.Scanner. (.getInputStream socket))
            o-stream (java.io.PrintWriter. 
                       (.getOutputStream socket) true)
            headers (parse-headers scanner)]
        (.print o-stream (build-response "Clojure Echo Server" 
                                         (:path headers)))
        (.flush o-stream)))
      (recur)))

