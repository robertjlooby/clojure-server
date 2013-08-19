(ns clojure_server.cob-spec-server
  (:require [clojure_server.server :refer :all]
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
                body-seq
                ["</body>"
                 "</html>"])))

(defn write-form [path request]
  (write-body-to-file path (:body request))
  (serve-file path))

(defn -main [& args]
  (with-command-line args
    "Usage...."
    [[port p "The port to run the server on" "3000"]
     [directory d "The directory to serve files from"]]
  (let [form (clojure.java.io/file directory "form")]
    (.createNewFile form)
    (.deleteOnExit form))
  (with-open [server-socket (create-server-socket (read-string port)
                            (java.net.InetAddress/getByName "localhost"))]
    (defrouter router [request params]
      (GET "/" (serve-file directory))
      (PUT "/form"  (write-form (str directory "/form") request))
      (POST "/form" (write-form (str directory "/form") request))
      (OPTIONS "/method_options" [{:headers {:allow "GET,HEAD,POST,OPTIONS,PUT"}} 200])
      (GET "/redirect" [{:headers {:Location (str "http://localhost:" port "/")}} 301])
      (GET "/logs" [{:content '("Authentication required")} 401])
      (GET "/:file" (serve-file (str directory "/" (:file params)))))
    (server server-socket directory router))))
