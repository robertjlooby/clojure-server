(ns clojure_server.response-builder-spec
  (:require [speclj.core :refer :all]
            [clojure_server.response-builder :refer :all]))

(describe "response builder"
  (it "should have a response line"
    (let [response (build-response "title" "content")]
      (should= "HTTP/1.1 200 OK" 
                      (first (clojure.string/split response #"\r\n")))))

  (it "should have the given title"
    (let [response (build-response "title" "content")]
      (should-contain "<title>title</title>" response)))

  (it "should have the given content"
    (let [response (build-response "title" "content")]
      (should-contain #"<body>\s*content\s*</body>" response)))

)
