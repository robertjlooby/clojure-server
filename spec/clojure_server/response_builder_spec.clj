(ns clojure_server.response-builder-spec
  (:use [clojure.string :only [join]])
  (:require [speclj.core :refer :all]
            [clojure_server.response-builder :refer :all]))

(defn str-to-seq [string]
  (line-seq (java.io.BufferedReader. 
              (java.io.StringReader. string))))

(describe "response builder"
  (it "should have a response line"
    (let [response (build-response [(str-to-seq "hello\n") 200])]
      (should= "HTTP/1.1 200 OK" 
                      (first response))))

  (it "should provide full correct response"
    (let [response (build-response [(str-to-seq "hello\nworld\n") 200])]
      (should= (seq ["HTTP/1.1 200 OK" "" "hello" "world" ""])
               response)))
)
