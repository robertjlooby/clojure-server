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
  (serve-file path))

(defn get-logs [request]
  (let [auth (:Authorization (:headers request))]
    (if-not auth [{:content '("Authentication required")} 401]
      (let [pass (second (clojure.string/split auth #" "))
            decoded (apply str
                           (map char
                                (decode 
                                  (bytes 
                                    (byte-array 
                                      (map 
                                        (comp byte int) pass))))))]
        (if (= decoded "admin:hunter2")
          [{:content '("GET /log HTTP/1.1"
                       "PUT /these HTTP/1.1"
                       "HEAD /requests HTTP/1.1")} 200]
          [{:content '("Authentication required")} 401])))))


(defn -main [& args]
  (with-command-line args
    "Usage...."
    [[port p "The port to run the server on" "3000"]
     [directory d "The directory to serve files from"]]
  (let [form (clojure.java.io/file directory "form")]
    (.createNewFile form)
    (.deleteOnExit form))
  (with-open [server-socket (create-server-socket (Integer/parseInt port)
                            (java.net.InetAddress/getByName "localhost"))]
    (defrouter router [request params]
      (GET "/" (serve-file directory))
      (PUT "/form"  (write-form (str directory "/form") request))
      (POST "/form" (write-form (str directory "/form") request))
      (OPTIONS "/method_options" [{:headers {:allow "GET,HEAD,POST,OPTIONS,PUT"}} 200])
      (GET "/redirect" [{:headers {:Location (str "http://localhost:" port "/")}} 301])
      (GET "/logs" (get-logs request))
      (GET "/:file" (serve-file (str directory "/" (:file params)))))
    (server server-socket directory router))))
