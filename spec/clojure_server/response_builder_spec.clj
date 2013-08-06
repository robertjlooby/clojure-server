(ns clojure_server.response-builder-spec
  (:use [clojure.string :only [join]])
  (:require [speclj.core :refer :all]
            [clojure_server.response-builder :refer :all]))

(describe "response builder"
  (it "should have a response line"
    (let [response (build-response "title" "content")]
      (should= "HTTP/1.1 200 OK" 
                      (first response))))

  (it "should have the given title"
    (let [response (build-response "title" "content")]
      (should-contain "<title>title</title>" (join response))))

  (it "should have the given content"
    (let [response (build-response "title" "content")]
      (should-contain #"<body>\s*content\s*</body>" (join response))))

)
