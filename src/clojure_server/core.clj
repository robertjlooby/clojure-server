(ns clojure_server.core
  (:require [clojure_server.header-parser :refer :all]
            [clojure_server.response-builder :refer :all]
            [clojure_server.router :refer :all])
  (:import java.net.ServerSocket))

(defn create-server-socket 
  ([port] (java.net.ServerSocket. port))
  ([port address] (java.net.ServerSocket. port 0 address)))

(defn listen [server-socket]
  (try
    (.accept server-socket)
    (catch Exception e (prn (str "exception caught " e)))))

(defn socket-out-writer [socket]
  (java.io.PrintWriter.
    (.getOutputStream socket) true))

(defn reader-seq [reader]
  (let [in-buffer (java.io.BufferedReader. reader)]
    (repeatedly #(.readLine in-buffer))))

(defn socket-in-seq [socket]
  (reader-seq (java.io.InputStreamReader. (.getInputStream socket))))

(defn file-in-seq [file-path]
  (reader-seq (java.io.FileReader. 
                (clojure.java.io/file file-path))))

(defn echo-server [server-socket]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [i-stream (socket-in-seq socket)
            o-stream (socket-out-writer socket)
            headers  (parse-headers i-stream)
            response (build-response [(seq [(:path headers)]) 200])]
        (doseq [line response]
          (.println o-stream line))))
    (if (.isClosed server-socket) (prn "echo-server exiting, socket closed") (recur))))

(defn server [server-socket directory router]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [i-stream (socket-in-seq socket)
            o-stream (socket-out-writer socket)
            headers  (parse-headers i-stream)
            router-response (router headers)
            response (build-response router-response)]
        (doseq [line response]
          (.println o-stream line))))
    (if (.isClosed server-socket) (prn "server exiting, socket closed") (recur))))
