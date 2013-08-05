(ns clojure_server.server
  (:require [clojure_server.core :refer :all]))

(with-open [server-socket (create-server-socket 3000
                          (java.net.InetAddress/getByName "localhost"))]
  (echo-server server-socket))
