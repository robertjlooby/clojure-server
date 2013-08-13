(ns clojure_server.router-spec
  (:require [speclj.core :refer :all]
            [clojure_server.router :refer :all]))

(describe "router"
  (it "should be able to match exact route names"
    (defrouter my-router [request params]
      (GET "/hello" ["hello route", 200])
      (POST "/heythere" ["heythere route", 200]))
    (should= ["hello route", 200] 
             (my-router {:method "GET" :path "/hello"}))
    (should= ["heythere route", 200]
             (my-router {:method "POST" :path "/heythere"})))

  (it "should call functions to generate response"
    (defn generator [method path]
      [(str method " " path), 200])
    (defrouter my-router [request params]
      (GET "/testpath" (generator (:method request) (:path request))))
    (should= ["GET /testpath", 200]
             (my-router {:method "GET" :path "/testpath"})))
)
