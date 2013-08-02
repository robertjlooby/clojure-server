(ns clojure_server.server
  (:require [clojure_server.core :refer :all]))

(echo-server 3000 (java.net.InetAddress/getByName "localhost"))
