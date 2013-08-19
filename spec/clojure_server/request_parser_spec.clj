(ns clojure_server.request-parser-spec
  (:require [speclj.core :refer :all]
            [clojure_server.request-parser :refer :all]))

(describe "read-until-emptyline"
  (with reader (clojure.java.io/reader 
                 (java.io.StringReader.
                   "first\r\nsecond\r\nthird\r\n\r\nnot me!\r\n")))

  (it "reads up to the first empty line"
    (should= '("first" "second" "third")
             (read-until-emptyline @reader))
    (should= '("not me!")
             (read-until-emptyline @reader)))
)

(describe "read-n-bytes"
  (with reader (clojure.java.io/reader 
                 (java.io.StringReader.
                   "first\r\nsecond\r\nthird\r\n\r\n")))

  (it "reads n bytes of the string"
    (should= '("first") (read-n-bytes @reader 5)))

  (it "reads n bytes of the string into seq of lines"
    (should= '("first" "second" "th") (read-n-bytes @reader 17)))

  (it "reads to end of input"
    (should= '("first" "second" "third" "")
             (read-n-bytes @reader 24)))

  (it "only reads to end of input"
    (should= '("first" "second" "third" "")
             (read-n-bytes @reader 25)))

  (it "only reads to end of input, even for long num-bytes"
    (should= '("first" "second" "third" "")
             (read-n-bytes @reader 250)))
)

(describe "parse-headers"
  (with get-seq '("GET /helloworld HTTP/1.1"
                  "Accept: text/html"
                  "Connection: keep-alive"))
  (with post-seq '("POST /helloworldform HTTP/1.1"
                   "Content-Length: 10"))

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
)

(describe "parse-request"
  (with reader 
   (java.io.StringReader.
    "GET /hello HTTP/1.1\r\nContent-Length: 10\r\n\r\nbody\r\ntest"))
  (with no-body-reader 
   (java.io.StringReader.
    "GET /hello HTTP/1.1\r\n\r\n"))
  (it "should get all the headers"
    (let [request (parse-request @reader)]
      (should= "GET" (:method (:headers request)))
      (should= "/hello" (:path (:headers request)))
      (should= "HTTP/1.1" (:http-version (:headers request)))
      (should= "10" (:Content-Length (:headers request)))))

  (it "should get the body"
    (let [request (parse-request @reader)]
      (should= '("body" "test") (:body request))))

  (it "should have an empty body if no content length"
    (let [request (parse-request @no-body-reader)]
      (should= '() (:body request))))
)
