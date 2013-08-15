(ns clojure_server.header-parser-spec
  (:require [speclj.core :refer :all]
            [clojure_server.header-parser :refer :all]))

(describe "header parser"
  (with get-seq '("GET /helloworld HTTP/1.1"
                  "Accept: text/html"
                  "Connection: keep-alive"
                  ""))
  (with post-seq '("POST /helloworldform HTTP/1.1"
                   "Content-Length: 10"
                   ""
                   "hello"
                   "world"
                   ""))

  (it "should get GET requests"
    (let [headers (parse-headers @get-seq)]
      (should= "GET" (:method headers))))

  (it "should get POST requests"
    (let [headers (parse-headers @post-seq)]
      (should= "POST" (:method headers))))
  
  (it "should get path for get"
    (let [headers (parse-headers @get-seq)]
      (should= "/helloworld" (:path headers))))

  (it "should get path for post"
    (let [headers (parse-headers @post-seq)]
      (should= "/helloworldform" (:path headers))))

  (it "should get HTTP version"
    (let [headers (parse-headers @get-seq)]
      (should= "HTTP/1.1" (:http-version headers))))

  (it "should get all headers"
    (let [headers (parse-headers @get-seq)]
      (should= "keep-alive" (:Connection headers))
      (should= "text/html" (:Accept headers))))

  (it "should get the body when Content-Length present"
    (let [headers (parse-headers @post-seq)]
      (should= "10" (:Content-Length headers))
      (should= '("hello" "world") (:body headers))))
)
