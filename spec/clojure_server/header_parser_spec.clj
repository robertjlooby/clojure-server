(ns clojure_server.header-parser-spec
  (:require [speclj.core :refer :all]
            [clojure_server.header-parser :refer :all]))

(describe "header parser"
  (with get-scanner (java.util.Scanner. 
                      (str "GET /helloworld HTTP/1.1\r\n"
                           "\r\n")))
  (with post-scanner (java.util.Scanner. 
                       (str "POST /helloworldform HTTP/1.1\r\n"
                            "\r\n")))

  (it "should get GET requests"
    (let [headers (parse-headers @get-scanner)]
      (should= "GET" (:method headers))))

  (it "should get POST requests"
    (let [headers (parse-headers @post-scanner)]
      (should= "POST" (:method headers))))
  
  (it "should get path for get"
    (let [headers (parse-headers @get-scanner)]
      (should= "/helloworld" (:path headers))))

  (it "should get path for post"
    (let [headers (parse-headers @post-scanner)]
      (should= "/helloworldform" (:path headers))))

  (it "should get HTTP version"
    (let [headers (parse-headers @get-scanner)]
      (should= "HTTP/1.1" (:http-version headers))))
)
