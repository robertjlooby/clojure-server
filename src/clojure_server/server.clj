(ns clojure_server.server
  (:require [clojure_server.core :refer :all]
            [clojure_server.router :refer :all])
  (:use clojure.contrib.command-line)
  (:gen-class))

(defn -main [& args]
  (with-command-line args
    "Usage...."
    [[port p "The port to run the server on" "3000"]
     [directory d "The directory to serve files from"]]
  (with-open [server-socket (create-server-socket (read-string port)
                            (java.net.InetAddress/getByName "localhost"))]
    (defrouter router [request params]
      (GET "/" [(serve-directory directory) 200]))
    (server server-socket directory router))))
