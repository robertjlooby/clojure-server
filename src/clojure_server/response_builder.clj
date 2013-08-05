(ns clojure_server.response-builder)

(defn build-response [title, content]
  (clojure.string/join
    (map #(str % "\r\n")
      ["HTTP/1.1 200 OK"
       ""
       "<!DOCTYPE html>"
       "<html>"
       "<head>"
       (str "<title>" title "</title>")
       "</head>"
       "<body>"
       content
       "</body>"
       "</html>"
       ""])))
