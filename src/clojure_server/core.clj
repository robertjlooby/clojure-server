(ns clojure_server.core
  (:require [clojure_server.header-parser :refer :all]
            [clojure_server.response-builder :refer :all])
  (:import java.net.ServerSocket))

(defn create-server-socket 
  ([port] (java.net.ServerSocket. port))
  ([port address] (java.net.ServerSocket. port 0 address)))

(defn listen [server-socket]
  (.accept server-socket))

(defn socket-out-writer [socket]
  (java.io.PrintWriter.
    (.getOutputStream socket) true))

(defn socket-in-seq [socket]
  (let [in-buffer (java.io.BufferedReader.
                    (java.io.InputStreamReader.
                      (.getInputStream socket)))]
    (repeatedly #(.readLine in-buffer))))

(defn echo-server [server-socket]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [i-stream (socket-in-seq socket)
            o-stream (socket-out-writer socket)
            headers  (parse-headers i-stream)
            response (build-response "Clojure Echo Server" 
                             (:path headers))]
        (doseq [line response]
          (.println o-stream line))))
    (recur)))
