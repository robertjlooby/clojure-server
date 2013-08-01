(ns clojure_server.core-spec
  (:require [speclj.core :refer :all]
            [clojure_server.core :refer :all]))

(describe "server-socket"
  (it "should create a java.net.ServerSocket"
    (let [socket (server-socket 3000)]
      (should= java.net.ServerSocket (class socket))
      (.close socket)))

  (it "should listen to the given port"
    (let [socket (server-socket 3000)]
      (should= 3000 (.getLocalPort socket))
      (.close socket)))
)
