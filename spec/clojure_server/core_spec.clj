(ns clojure_server.core-spec
  (:require [speclj.core :refer :all]
            [clojure_server.core :refer :all]
            [clojure_server.router :refer :all])
  (:import java.net.Socket
           java.io.File))

(defn connect-socket [addr port]
  (try
    (Socket. addr port)
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

(describe "seq-to-file"
  (it "writes the seq of strings to a file"
    (let [f (java.io.File/createTempFile "temp" ".html")]
      (seq-to-file f '("this" "is" "text"))
      (should= '("this" "is" "text") (file-to-seq
                                       (.getAbsolutePath f))))) 
)

(describe "serve-file"
  (with dirpath (.getAbsolutePath (File.
                                  (.getAbsolutePath (File. ""))
                                  "public")))
  (with goodpath (.getAbsolutePath (File. @dirpath "file1")))
  (with badpath (.getAbsolutePath (File. @dirpath "file1000")))

  (it "should return vector response"
    (should= (class []) (class (serve-file @goodpath)))
    (should= true (seq? (:content (first (serve-file @goodpath)))))
    (should= (class 200) (class (second (serve-file @goodpath))))
    (should= 2 (count (serve-file @goodpath))))

  (it "should have the contents of the file"
    (should-contain "file1 contents" (:content (first (serve-file @goodpath))))
    (should= 200 (second (serve-file @goodpath))))

  (it "should return a 404 for bad path"
    (should= 404 (second (serve-file @badpath))))

  (it "should have the name of the directory served"
    (should-contain @dirpath (:content (first (serve-file @dirpath))))
    (should= 200 (second (serve-file @dirpath))))

  (it "should have links to files in directory"
    (should-contain "<div><a href=\"/image.gif\">image.gif</a></div>"
                    (:content (first (serve-file @dirpath))))
    (should-contain "<div><a href=\"/file1\">file1</a></div>"
                    (:content (first (serve-file @dirpath)))))
  
  (it "should serve as HTML page"
    (should-contain "<!DOCTYPE html>"
                    (:content (first (serve-file @dirpath))))
    (should-contain "<body>"
                    (:content (first (serve-file @dirpath)))))
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
            (let [headers (doall (read-until-emptyline i-stream))]
              (should-contain "HTTP/1.1 200 OK" headers))
            (should-contain "/helloworld" 
                            (read-until-emptyline i-stream)))))))
)

(describe "server"
  (it "listens to the socket and can serve a static directory"
    (let [path (.getAbsolutePath (File.
                                    (.getAbsolutePath (File. ""))
                                    "public"))
          addr (java.net.InetAddress/getByName "localhost")]
      (with-open [server-socket (create-server-socket 3000 addr)]
        (defrouter router [request params]
          (GET "/"(serve-file path)))
        (future (server server-socket path router))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET / HTTP/1.1\r\n")
            (let [headers (doall (read-until-emptyline i-stream))]
              (should-contain "HTTP/1.1 200 OK" headers))
            (should-contain path
                            (read-until-emptyline i-stream))))
        (with-open [client-socket (connect-socket addr 3000)]
          (let [i-stream (socket-reader client-socket)
                o-stream (socket-writer client-socket)]
            (.println o-stream "GET /foobar HTTP/1.1\r\n")
            (should-contain "HTTP/1.1 404 Not Found" 
                            (read-until-emptyline i-stream)))))))
)
