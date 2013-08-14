(ns clojure_server.response-builder-spec
  (:use [clojure.string :only [join]])
  (:require [speclj.core :refer :all]
            [clojure_server.response-builder :refer :all]))

(defn str-to-seq [string]
  (line-seq (java.io.BufferedReader. 
              (java.io.StringReader. string))))
(describe "status-code-converter"
  (it "should return '200 OK' for 200"
    (should= "200 OK" (status-code-converter 200)))

  (it "should return '401 Unauthorized' for 401"
    (should= "401 Unauthorized" (status-code-converter 401)))

  (it "should return '404 Not Found' for 404"
    (should= "404 Not Found" (status-code-converter 404)))

  (it "should return '301 Moved Permanently' for 301"
    (should= "301 Moved Permanently" (status-code-converter 301)))
)

(describe "response builder"
  (it "should have a response line"
    (let [response (build-response [{:content (str-to-seq "hello\n")} 200])]
      (should= "HTTP/1.1 200 OK" 
                      (first response))))

  (it "should provide full correct response"
    (let [response (build-response [{:content (str-to-seq "hello\nworld\n")} 200])]
      (should= (seq ["HTTP/1.1 200 OK" "" "hello" "world" ""])
               response)))

  (it "should give the correct response for a 404 Error"
    (let [response (build-response [{:content '("Not Found")} 404])]
      (should= (seq ["HTTP/1.1 404 Not Found" "" "Not Found" ""])
               response)))

  (it "should include headers"
    (let [response (build-response [{:headers {:allow "GET"}} 200])]
      (should= "allow: GET" 
                      (second response))))

  (it "should include multiple headers"
    (let [response (build-response [{:headers 
                                     {:allow "GET"
                                      :Location "localhost"}} 200])]
      (should-contain "allow: GET" 
                      (take 3 response))
      (should-contain "Location: localhost" 
                      (take 3 response))))
)
