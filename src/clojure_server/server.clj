(ns clojure_server.server
  (:require [clojure_server.request-parser :refer :all]
            [clojure_server.response-builder :refer :all]
            [clojure_server.router :refer :all]))

(defn create-server-socket 
  ([port] (java.net.ServerSocket. port))
  ([port address] (java.net.ServerSocket. port 0 address)))

(defn listen [server-socket]
  (try
    (.accept server-socket)
    (catch Exception e (prn (str "exception caught " e)))))

(defn socket-writer [socket]
  (java.io.PrintWriter.
    (.getOutputStream socket) true))

(defn file-to-seq [file-path]
  (line-seq (java.io.BufferedReader. 
              (java.io.FileReader. 
                (clojure.java.io/file file-path)))))

(defn seq-to-file [file string-seq]
  (let [p (java.io.PrintWriter. file)]
    (doseq [line string-seq]
      (.println p line))
    (.flush p)))

(defn serve-directory [dir]
  (concat
    ["<!DOCTYPE html>"
     "<html>"
     "<head>" 
     "</head>"
     "<body>"
     (.getAbsolutePath dir)]
    (map #(str "<div><a href=\"/" (.getName %) "\">" (.getName %) "</a></div>")
      (.listFiles dir))
    ["</body>"
     "</html>"]))

(defn serve-file [path]
  (let [file (clojure.java.io/file path)]
    (if (.exists file)
      (cond
        (.isDirectory file)
          [{:content (serve-directory file)} 200]
        :else
          [{:content (file-to-seq path)} 200])
      [{:content '("Not Found")} 404])))

(defn echo-server [server-socket]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [i-stream (socket-reader socket)
            o-stream (socket-writer socket)
            headers  (parse-headers
                       (read-until-emptyline i-stream))
            response (build-response [{:content (seq [(:path headers)])} 200])]
        (doseq [line response]
          (.println o-stream line))))
    (if (.isClosed server-socket) (prn "echo-server exiting, socket closed") (recur))))

(defn server [server-socket directory router]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [i-stream (socket-reader socket)
            o-stream (socket-writer socket)
            headers  (parse-headers
                       (read-until-emptyline i-stream))
            router-response (router headers)
            response (build-response router-response)]
        (doseq [line response]
          (.println o-stream line))))
    (if (.isClosed server-socket) (prn "server exiting, socket closed") (recur))))
