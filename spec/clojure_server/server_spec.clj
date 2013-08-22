(ns clojure_server.server-spec
  (:require [speclj.core :refer :all]
            [clojure_server.request-parser :refer :all]
            [clojure_server.response-builder :refer :all]
            [clojure_server.router :refer :all]
            [clojure_server.server :refer :all]))

(defn connect-socket [addr port]
  (try
    (java.net.Socket. addr port)
    (catch java.net.ConnectException e 
      (connect-socket addr port))))

(describe "create-server-socket"
  (it "should create a java.net.ServerSocket"
    (with-open [socket (create-server-socket 3000)]
      (should= java.net.ServerSocket (class socket))))

  (it "should connect to the given port"
    (with-open [socket (create-server-socket 3000)]
      (should= 3000 (.getLocalPort socket))))

  (it "should connect to the given InetAddress"
    (let [addr (java.net.InetAddress/getByName "localhost")]
      (with-open [socket (create-server-socket 3000 addr)]
        (should= addr (.getInetAddress socket)))))
)
  
(describe "listen"
  (it "listens to the given server socket and returns a socket when connected to"
    (let [addr (java.net.InetAddress/getByName "localhost")]
      (with-open [server-socket (create-server-socket 3000 addr)]
        (let [sockets (doall 
                        (pvalues (listen server-socket)
                                 (connect-socket addr 3000)))]
          (with-open [server-side-socket (first sockets)
                      client-side-socket (second sockets)]
            (should= java.net.Socket (class server-side-socket))
            (should= (.getLocalPort client-side-socket) 
                     (.getPort server-side-socket))
            (should= (.getPort client-side-socket) 
                     (.getLocalPort server-side-socket)))))))
)

(describe "seq-to-file"
  (it "writes the seq of strings to a file"
    (let [f (java.io.File/createTempFile "temp" ".html")]
      (seq-to-file f '("this" "is" "text"))
      (should= '("this" "is" "text") (file-to-seq
                                       (.getAbsolutePath f))))) 
)

(describe "serve-file"
  (with dirpath (.getAbsolutePath (clojure.java.io/file
                                  (.getAbsolutePath (clojure.java.io/file ""))
                                  "public")))
  (with goodpath (.getAbsolutePath (clojure.java.io/file @dirpath "file1")))
  (with badpath (.getAbsolutePath (clojure.java.io/file @dirpath "file1000")))
  (with gifpath (.getAbsolutePath (clojure.java.io/file @dirpath "image.gif")))
  (with partial-path (.getAbsolutePath (clojure.java.io/file @dirpath "partial_content.txt")))

  (it "should return vector response"
    (should= (class []) (class (serve-file @goodpath {}))))

  (it "should have a :content-stream"
    (should= true (isa? (class
                          (:content-stream
                            (first (serve-file @goodpath {}))))
                        java.io.InputStream)))

  (it "should have a status code"
    (should= (class 200) (class (second (serve-file @goodpath {})))))

  (it "should just have the response map and status code"
    (should= 2 (count (serve-file @goodpath {}))))

  (it "should have the contents of the file"
    (let [stream (:content-stream (first (serve-file @goodpath {})))
          b-a (byte-array 5000)
          _ (.read stream b-a 0 5000)]
    (should-contain "file1 contents" (String. b-a))
    (should= 200 (second (serve-file @goodpath {})))))

  (it "should return a 404 for bad path"
    (should= 404 (second (serve-file @badpath {}))))

  (it "should return 206 for partial content"
    (should= 206 (second (serve-file @partial-path {:headers
                                                    {:Range "bytes=0-4"}}))))

  (it "should have the name of the directory served"
    (let [stream (:content-stream (first (serve-file @dirpath {})))
          b-a (byte-array 5000)
          _ (.read stream b-a 0 5000)]
    (should-contain @dirpath (String. b-a))
    (should= 200 (second (serve-file @dirpath {})))))

  (it "should have links to files in directory"
    (let [stream (:content-stream (first (serve-file @dirpath {})))
          b-a (byte-array 5000)
          _ (.read stream b-a 0 5000)]
    (should-contain
      "<div><a href=\"/image.gif\">image.gif</a></div>"
      (String. b-a))
    (should-contain
      "<div><a href=\"/file1\">file1</a></div>"
      (String. b-a))
    (should= 200 (second (serve-file @dirpath {})))))
  
  (it "should serve as HTML page"
    (let [stream (:content-stream (first (serve-file @dirpath {})))
          b-a (byte-array 5000)
          _ (.read stream b-a 0 5000)]
    (should-contain
      "<!DOCTYPE html>"
      (String. b-a))
    (should-contain
      "<body>"
      (String. b-a))))

  (it "should have a header of :media-type 'image/gif' for gif files"
    (should= "image/gif"
                    (:media-type (:headers 
                                (first (serve-file @gifpath {}))))))
  (it "should have a header of :Content-Length 14 for file1"
    (should= 14
              (:Content-Length (:headers 
                          (first (serve-file @goodpath {}))))))
)

(describe "extension"
  (it "should return nil for '/'"
    (should= nil (extension "/")))

  (it "should return .html for '/file.html'"
    (should= ".html" (extension "/file.html")))

  (it "should return .gif for '/path/to/file.gif'"
    (should= ".gif" (extension "/path/to/file.gif")))

  (it "should return .png for '/even.with.periods.png'"
    (should= ".png" (extension "/even.with.periods.png")))
)

(describe "echo-server"
  (it "listens to the socket and echos the pathname"
    (let [addr (java.net.InetAddress/getByName "localhost")]
      (with-open [server-socket (create-server-socket 3000 addr)]
        (future (echo-server server-socket))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET /helloworld HTTP/1.1\r\n")
            (should-contain "HTTP/1.1 200 OK" 
                            (doall (read-until-emptyline i-stream)))
            (should-contain "/helloworld" 
                            (read-until-emptyline i-stream)))))))
)

(describe "server"
  (it "listens to the socket and can serve a static directory"
    (let [path (.getAbsolutePath (clojure.java.io/file
                                    (.getAbsolutePath (clojure.java.io/file ""))
                                    "public"))
          addr (java.net.InetAddress/getByName "localhost")]
      (with-open [server-socket (create-server-socket 3000 addr)]
        (defrouter test-router [request params]
          (GET "/"(serve-file path request))
          (GET "/file1" (serve-file (str path "/file1") request))
          (GET "/image.gif" (serve-file (str path "/image.gif") request)))
        (future (server server-socket path test-router))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET / HTTP/1.1\r\n")
            (should-contain "HTTP/1.1 200 OK"
                            (doall (read-until-emptyline i-stream)))
            (should-contain path
                            (first
                              (read-until-emptyline i-stream)))))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET /file1 HTTP/1.1")
            (.println o-stream "Range: bytes=0-4\r\n")
            (should-contain "HTTP/1.1 206 Partial Content"
                            (doall (read-until-emptyline i-stream)))
            (should= '("file")
                            (read-n-bytes i-stream 100))))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET /file1 HTTP/1.1")
            (.println o-stream "Range: bytes=4-9\r\n")
            (should-contain "HTTP/1.1 206 Partial Content"
                            (doall (read-until-emptyline i-stream)))
            (should= '("1 con")
                            (read-n-bytes i-stream 100))))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET /image.gif HTTP/1.1\r\n")
            (should-contain "HTTP/1.1 200 OK"
                            (read-until-emptyline i-stream))
            (should-contain "media-type: image/gif"
                            (doall (read-until-emptyline i-stream)))))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET /foobar HTTP/1.1\r\n")
            (should-contain "HTTP/1.1 404 Not Found" 
                            (read-until-emptyline i-stream)))))))
)
