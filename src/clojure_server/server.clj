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

(defn serve-file [path request]
  (let [file (clojure.java.io/file path)]
    (if (.exists file)
      (cond
        (.isDirectory file)
          [{:content (serve-directory file)} 200]
        (= ".gif" (apply str (take-last 4 path)))
          [{:headers {:media-type "image/gif"
                     :Content-Length (.length file)}
            :image-file file} 200]
        (= ".png" (apply str (take-last 4 path)))
          [{:headers {:media-type "image/png"
                     :Content-Length (.length file)}
            :image-file file} 200]
        (= ".jpeg" (apply str (take-last 5 path)))
          [{:headers {:media-type "image/jpeg"
                     :Content-Length (.length file)}
            :image-file file} 200]
        (:Range (:headers request))
          (let [[_ f l] (first (re-seq #"bytes=(\d+)-(\d+)"
                                (:Range (:headers request))))
                begin (Integer/parseInt f)
                end   (Integer/parseInt l)
                reader (clojure.java.io/reader file)
                _ (read-n-bytes reader begin)]
            [{:content (read-n-bytes reader (- end begin))} 206])
        :else
          [{:content (file-to-seq path)} 200])
      [{:content '("Not Found")} 404])))

(defn echo-server [server-socket]
  (loop []
    (with-open [socket (listen server-socket)]
      (let [o-stream (socket-writer socket)
            request  (parse-request socket)
            response (build-response 
                       [{:content [(:path (:headers request))]} 200])]
        (doseq [line response]
          (.println o-stream line))))
    (if (.isClosed server-socket) (prn "echo-server exiting, socket closed") (recur))))

(defn server [server-socket directory router]
  (loop []
    (let [socket-to-client (listen server-socket)]
      (future
        (with-open [socket socket-to-client]
          (let [o-stream (socket-writer socket)
                request  (parse-request socket)
                router-response (router request)
                response (build-response router-response)]
            (cond
              (contains? #{"image/gif" "image/jpeg" "image/png"}
                         (:media-type
                               (:headers
                                 (first router-response))))
              (let [image-file (:image-file (first router-response))
                    headers (butlast response)
                    f-i-stream (java.io.FileInputStream. image-file)
                    s-o-stream (.getOutputStream socket)
                    chunk-size 1024
                    b-a (byte-array chunk-size)]
                (doseq [line headers]
                  (.println o-stream line))
                (loop [num-read (.read f-i-stream b-a 0 chunk-size)]
                  (prn "num-read=" num-read)
                  (cond
                    (= -1 num-read)
                      (.flush s-o-stream)
                    :else
                    (do
                      (.write s-o-stream b-a 0 num-read)
                      (recur (.read f-i-stream b-a 0 chunk-size))))))
              (re-matches #".*206.*" (first response))
              (let [to-print (butlast response)]
                (doseq [line (butlast to-print)]
                  (.println o-stream line))
                (.print o-stream (last to-print))
                (.flush o-stream))
              :else
              (doseq [line response]
                (.println o-stream line)))))))
    (if (.isClosed server-socket) (prn "server exiting, socket closed") (recur))))
