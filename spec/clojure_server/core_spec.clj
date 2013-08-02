(ns clojure_server.core-spec
  (:require [speclj.core :refer :all]
            [clojure_server.core :refer :all])
  (:import java.net.Socket))

(describe "create-server-socket"
  (it "should create a java.net.ServerSocket"
    (let [socket (create-server-socket 3000)]
      (should= java.net.ServerSocket (class socket))
      (.close socket)))

  (it "should connect to the given port"
    (let [socket (create-server-socket 3000)]
      (should= 3000 (.getLocalPort socket))
      (.close socket)))

  (it "should connect to the given InetAddress"
    (let [addr (java.net.InetAddress/getByName "localhost")
          socket (create-server-socket 3000 addr)]
      (should= addr (.getInetAddress socket))
      (.close socket)))
)
  
(describe "listen"
  (it "listens to the given server socket and returns a socket when connected to"
    (let [addr (java.net.InetAddress/getByName "localhost")
          server-socket (create-server-socket 3000 addr)
          future-server-socket (future (listen server-socket))
          future-client-socket (future (Socket. addr 3000))
          server-side-socket @future-server-socket
          client-side-socket @future-client-socket]
        (should= java.net.Socket (class server-side-socket))
        (should= (.getLocalPort client-side-socket) (.getPort server-side-socket))
        (should= (.getPort client-side-socket) (.getLocalPort server-side-socket))
        (.close server-side-socket)
        (.close client-side-socket)
        (.close server-socket)))
)

(describe "echo-server"
  (it "listens to the socket and echos the pathname"
    (let [addr (java.net.InetAddress/getByName "localhost")
          server (future (echo-server 3000 addr))
          future-client-socket (future (Socket. addr 3000))
          client-side-socket @future-client-socket
          scanner (java.util.Scanner. (.getInputStream client-side-socket))
          o-stream (java.io.PrintWriter. (.getOutputStream client-side-socket) true)]
      (.println o-stream "GET /helloworld HTTP/1.1")
      (should= "helloworld" (.nextLine scanner))
      (.close client-side-socket)
      (future-cancel server)))
)
