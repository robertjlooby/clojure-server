(ns clojure_server.header-parser-spec
  (:require [speclj.core :refer :all]
            [clojure_server.header-parser :refer :all]))

(describe "header parser"
  (with get-reader (java.io.BufferedReader.
                  (java.io.StringReader.
                    (str "GET /helloworld HTTP/1.1\r\n"
                         "\r\n"))))
  (with post-reader (java.io.BufferedReader.
                   (java.io.StringReader. 
                     (str "POST /helloworldform HTTP/1.1\r\n"
                          "\r\n"))))

  (it "should get GET requests"
    (let [headers (parse-headers @get-reader)]
      (should= "GET" (:method headers))))

  (it "should get POST requests"
    (let [headers (parse-headers @post-reader)]
      (should= "POST" (:method headers))))
  
  (it "should get path for get"
    (let [headers (parse-headers @get-reader)]
      (should= "/helloworld" (:path headers))))

  (it "should get path for post"
    (let [headers (parse-headers @post-reader)]
      (should= "/helloworldform" (:path headers))))

  (it "should get HTTP version"
    (let [headers (parse-headers @get-reader)]
      (should= "HTTP/1.1" (:http-version headers))))
)
