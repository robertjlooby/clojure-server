(defproject clojure_server "0.1.0-SNAPSHOT"
  :description "An HTTP server implemented in Clojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :profiles {:dev {:dependencies [[speclj "2.5.0"]]}}
  :plugins [[speclj "2.5.0"]]
  :jvm-opts ["-Dline.separator=\r\n"]
  :main clojure_server.server
  :test-paths ["spec"])
