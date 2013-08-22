(ns clojure_server.cob-spec-server
  (:require [clojure.data.codec.base64 :refer :all]
            [clojure_server.server :refer :all]
            [clojure_server.router :refer :all])
  (:use clojure.contrib.command-line)
  (:gen-class))

(defn write-body-to-file [path body-seq]
  (seq-to-file (clojure.java.io/file path)
               (concat
                ["<!DOCTYPE html>"
                 "<html>"
                 "<head>" 
                 "</head>"
                 "<body>"]
                [(clojure.string/replace 
                  body-seq
                  #"="
                  " = ")]
                ["</body>"
                 "</html>"])))

(defn write-form [path request]
  (write-body-to-file path (:body request))
  (serve-file path request))

(defn get-logs [request]
  (let [auth (:Authorization (:headers request))]
    (if-not auth [{:content-stream (java.io.StringBufferInputStream.
                                     "Authentication required")} 401]
      (let [pass (second (clojure.string/split auth #" "))
            decoded (String. (decode (.getBytes pass)))]
        (if (= decoded "admin:hunter2")
          [{:content-stream
              (java.io.StringBufferInputStream.
                (str "GET /log HTTP/1.1\r\n"
                       "PUT /these HTTP/1.1\r\n"
                       "HEAD /requests HTTP/1.1\r\n"))} 200]
          [{:content-stream (java.io.StringBufferInputStream.
                                     "Authentication required")} 401])))))

(def directory (atom nil))
(def port (atom nil))
(defrouter router [request params]
  (GET "/" (serve-file @directory request))
  (PUT "/form"  (write-form (str @directory "/form") request))
  (POST "/form" (write-form (str @directory "/form") request))
  (OPTIONS "/method_options" [{:headers {:allow "GET,HEAD,POST,OPTIONS,PUT"}} 200])
  (GET "/redirect" [{:headers {:Location (str "http://localhost:" @port "/")}} 301])
  (GET "/logs" (get-logs request))
  (GET "/parameters" [{:content-stream
                         (java.io.StringBufferInputStream.
                          (clojure.string/join 
                            (map #(str (first %) " = " (second %) "\r\n") 
                                 params)))} 200])
  (GET "/:file" (serve-file (str @directory "/" (:file params)) request)))

(defn -main [& args]
  (with-command-line args
    "Usage...."
    [[p "The port to run the server on"]
     [d "The directory to serve files from"]]
    (reset! directory d)
    (reset! port (Integer/parseInt port))
    (let [form (clojure.java.io/file @directory "form")]
      (.createNewFile form)
      (.deleteOnExit form))
    (with-open [server-socket (create-server-socket @port
                              (java.net.InetAddress/getByName "localhost"))]
      (server server-socket @directory router))))
