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
  (line-seq (clojure.java.io/reader file-path)))

(defn seq-to-file [file string-seq]
  (let [p (java.io.PrintWriter. file)]
    (doseq [line string-seq]
      (.println p line))
    (.flush p)))

(defn serve-directory [dir]
  (java.io.StringBufferInputStream.
    (apply str
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
         "</html>"]))))

(defn extension [path]
  (let [start (.lastIndexOf path ".")]
    (if (> start 0)
      (subs path start))))

(defn serve-file [path request]
  (let [file (clojure.java.io/file path)]
    (if (.exists file)
      (cond
        (.isDirectory file)
          [{:content-stream (serve-directory file)} 200]
        (contains? #{".gif" ".png" ".jpeg"} (extension path))
          [{:headers {:media-type (str "image/"
                                       (subs (extension path) 1))
                     :Content-Length (.length file)}
            :content-stream (java.io.FileInputStream. file)} 200]
        (:Range (:headers request))
          (let [[_ f l] (first (re-seq #"bytes=(\d+)-(\d+)"
                                (:Range (:headers request))))
                begin (Integer/parseInt f)
                end   (Integer/parseInt l)
                reader (java.io.FileInputStream. file)
                _ (.read reader (byte-array begin) 0 begin)]
            [{:headers {:Content-Length (- end begin)}
              :content-stream reader} 206])
        :else
          [{:headers {:Content-Length (.length file)}
            :content-stream (java.io.FileInputStream. file)} 200])
      [{:headers {:Content-Length 9}
                  :content-stream
                    (java.io.StringBufferInputStream.
                      "Not Found")} 404])))

(defn echo-server [server-socket]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [o-stream (socket-writer socket)
            request  (parse-request socket)
            response (build-response [{} 200])
            full-response (concat response
                                  [(:path (:headers request)) ""])]
        (doseq [line full-response]
          (.println o-stream line))))
    (if (.isClosed server-socket) (prn "echo-server exiting, socket closed") (recur))))

(defn server [server-socket directory router]
  (loop []
    (let [socket-to-client (listen server-socket)]
      (future
        (with-open [socket socket-to-client]
          (let [p-o-stream (socket-writer socket)
                request  (parse-request socket)
                router-response (router request)
                headers (build-response router-response)]
            (doseq [line headers]
              (.println p-o-stream line))
            (let [i-stream (:content-stream (first router-response))
                  o-stream (.getOutputStream socket)
                  length (or (:Content-Length
                               (:headers (first router-response)))
                             Integer/MAX_VALUE)
                  chunk-size 1024
                  b-a (byte-array chunk-size)]
              (loop [num-read 
                        (.read i-stream b-a 0 (min length chunk-size))
                     tot-read 0]
                (cond
                  (< num-read chunk-size)
                    (do
                      (if (> num-read 0) (.write o-stream b-a 0 num-read))
                      (.flush o-stream))
                  :else
                    (do
                      (.write o-stream b-a 0 num-read)
                      (recur (.read i-stream b-a 0 
                                    (min (- length num-read tot-read) chunk-size))
                             (+ num-read tot-read))))))))))
    (if (.isClosed server-socket) (prn "server exiting, socket closed") (recur))))
