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

  (it "should return '404 Not Found' for 404"
    (should= "404 Not Found" (status-code-converter 404)))
)

(describe "response builder"
  (it "should have a response line"
    (let [response (build-response [(str-to-seq "hello\n") 200])]
      (should= "HTTP/1.1 200 OK" 
                      (first response))))

  (it "should provide full correct response"
    (let [response (build-response [(str-to-seq "hello\nworld\n") 200])]
      (should= (seq ["HTTP/1.1 200 OK" "" "hello" "world" ""])
               response)))

  (it "should give the correct response for a 404 Error"
    (let [response (build-response ['("Not Found") 404])]
      (should= (seq ["HTTP/1.1 404 Not Found" "" "Not Found" ""])
               response)))
)
