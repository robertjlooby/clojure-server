(ns clojure_server.router-spec
  (:require [speclj.core :refer :all]
            [clojure_server.router :refer :all]))

(describe "parse-router-path"
  (it "should return [''] for '/'"
   (should= [""] (parse-router-path "/")))

  (it "should return ['hello' 'world'] for '/hello/world'"
    (should= ["hello" "world"] (parse-router-path "/hello/world")))

  (it "should return ['hello' :id] for '/hello/:id'"
    (should= ["hello" :id] (parse-router-path "/hello/:id")))

  (it "should return ['a' :b :c 'd.html'] for '/a/:b/:c/d.html'"
    (should= ["a" :b :c "d.html"] (parse-router-path "/a/:b/:c/d.html")))
)

(describe "parse-request-path"
  (it "should return [[''] {}] for '/'"
    (should= [[""] {}] (parse-request-path "/")))

  (it "should return [['hello' 'world'] {}] for '/hello/world'"
    (should= [["hello" "world"] {}]
             (parse-request-path "/hello/world")))

  (it "should return [['file'] {'a' '123'}] for '/file?a=123'"
    (should= [["file"] {"a" "123"}]
             (parse-request-path "/file?a=123")))

  (it "should return [['file' 'path' 'user'] {'name' 'rob' 'id' '5'}] for '/file/path/user?name=rob&id=5'"
    (should= [["file" "path" "user"] {"name" "rob", "id" "5"}]
             (parse-request-path "/file/path/user?name=rob&id=5")))

  (it "should reutrn [['parameters'] {'var_1' 'Operators <, >''var_2' 'stuff'}] for '/parameters?var_1=Operators%20%3C%2C%20%3E&var_2=stuff'"
    (should= [["parameters"] {"var_1" "Operators <, >""var_2" "stuff"}]
             (parse-request-path "/parameters?var_1=Operators%20%3C%2C%20%3E&var_2=stuff")))
)

(describe "params-match"
  (it "should return nil when not a match"
      (should= nil (params-match "/" "/hello")))

  (it "should return an empty hash-map when it is a match"
      (should= {} (params-match "/hello" "/hello")))

  (it "should be able to match full string"
      (should= {} (params-match "/hello/world/deep/path.html" 
                                "/hello/world/deep/path.html")))

  (it "should match anything to a string and return it in the hash"
    (should= {:id "12345"} (params-match "/:id" "/12345")))

  (it "should match multiple params"
    (should= {:a "12" :b "xy" :c "3Z"} 
             (params-match "/root/:a/:b/space/:c/page.html" 
                           "/root/12/xy/space/3Z/page.html")))

  (it "should return nil even when params partially match, router longer"
    (should= nil (params-match "/:a/url/page" "/1234/url")))

  (it "should return nil even when params partially match, request longer"
    (should= nil (params-match "/:a/url/page" "/1234/url/page/user")))

  (it "should match params in the path and query"
    (should= {:user "rob" :id "123" "lang" "clojure"}
             (params-match "/path/:user/:id"
                           "/path/rob/123?lang=clojure")))
)

(describe "form-functionizer"
  (it "should return a string unmodified"
    (let [f "hello"
          fun (form-functionizer f arg arg2)]
      (should= "hello" (fun nil nil))))

  (it "should evaluate a function"
    (let [fun (form-functionizer (str "Hello " "World") arg arg2)]
      (should= "Hello World" (fun nil nil))))

  (it "should have access to first arg"
    (let [fun (form-functionizer (str "Hello " nme) nme arg2)]
      (should= "Hello Rob" (fun "Rob" nil))))

  (it "should have access to second arg"
    (let [fun (form-functionizer (+ a 5) arg1 a)]
      (should= 12 (fun nil 7))))

  (it "should have access to both args"
    (let [fun (form-functionizer (zipmap arg1 arg2) arg1 arg2)]
      (should= {:a 1 :b 2 :c 3} (fun [:a :b :c] [1 2 3]))))
)

(describe "request-matches"
  (it "should return falsy if path does not match"
    (should-not (request-matches "/" "/file1" "GET" "GET" (atom []))))

  (it "should return falsy if method does not match"
    (should-not (request-matches "/" "/" "PUT" "GET" (atom []))))

  (it "should return truthy if method and path match"
    (should (request-matches "/" "/" "GET" "GET" (atom []))))

  (it "should return truthy if method matches and path params matches"
    (should (request-matches "/:file" "/file1" "GET" "GET" (atom []))))

  (it "should return falsy if path params matches but methods don't match"
    (should-not (request-matches "/:file" "/file1" "PUT" "GET" (atom []))))

  (it "should return falsy, and add router-method to accept, if path matches but method does not"
    (let [accept (atom [])]
      (should-not (request-matches "/" "/" "PUT" "GET" accept))
      (should= ["PUT"] @accept)))
)

(describe "route-functionizer"
  (it "should return a function"
    (should= true (fn? (route-functionizer '(GET "/" "hello")
                                           'request 'params))))

  (it "should return last form element as a fn if first 2 match"
    (let [fun (route-functionizer '(GET "/" true) 'request 'params)
          fun2 (fun "GET" "/")]
      (should= true (fn? fun2))
      (should= true (fun2 nil nil))))

  (it "should match routes with params match"
    (let [fun (route-functionizer '(PUT "/path/:file/:user" "hello")
                                  'request 'params)
          fun2 (fun "PUT" "/path/file1/rob")]
      (should= "hello" (fun2 nil nil))))

  (it "should return nil if path does not match"
    (let [fun (route-functionizer '(PUT "/path/:file/:user" "hello")
                                  'request 'params)
          fun2 (fun "PUT" "/path2/file1/rob")]
      (should= nil fun2)))

  (it "should return the method if the path matches but not the method"
    (let [fun (route-functionizer '(PUT "/path/:file/:user" "hello")
                                  'request 'params)
          fun2 (fun "GET" "/path/file1/rob")]
      (should= "PUT" fun2)))

  (it "should evaluate last element of form if matches"
    (let [fun (route-functionizer '(GET "/path/:file/:user" (+ 2 2))
                                  'request 'params)
          fun2 (fun "GET" "/path/file1/rob")]
      (should= 4 (fun2 {} {}))))

  (it "should have access to request and params in last element of form"
    (let [fun (route-functionizer '(GET "/path/:file/:user" (+ request params))
                                  'request 'params)
          fun2 (fun "GET" "/path/file1/rob")]
      (should= 9 (fun2 5 4))))
)

(describe "router"
  (it "should be able to match exact route names"
    (defrouter my-router [request params]
      (GET "/hello" ["hello route", 200])
      (POST "/heythere" ["heythere route", 200]))
    (should= ["hello route", 200] 
             (my-router {:headers {:method "GET" :path "/hello"}}))
    (should= ["heythere route", 200]
             (my-router {:headers {:method "POST" :path "/heythere"}})))

  (it "should call functions to generate response"
    (defn generator [method path]
      [(str method " " path), 200])
    (defrouter my-router [request params]
      (GET "/testpath" (generator (:method (:headers request)) 
                                  (:path (:headers request)))))
    (should= ["GET /testpath", 200]
             (my-router {:headers {:method "GET" :path "/testpath"}})))

  (it "should match a path with a keyword param"
    (defrouter my-router [request params]
      (GET "/" ["root" 200])
      (GET "/:file" ["matched file" 200]))
    (should= ["root" 200]
             (my-router {:headers {:method "GET" :path "/"}}))
    (should= ["matched file" 200]
             (my-router {:headers {:method "GET" :path "/file1"}})))

  (it "should match a path with a multiple keyword params"
    (defrouter my-router [request params]
      (GET "/" ["root" 200])
      (GET "/:file"            ["matched 1 param"  200])
      (GET "/:file/:user"      ["matched 2 params" 200])
      (GET "/:file/:user/:num" ["matched 3 params" 200]))
    (should= ["root" 200]
             (my-router {:headers {:method "GET" :path "/"}}))
    (should= ["matched 1 param" 200]
             (my-router {:headers {:method "GET" :path "/file1"}}))
    (should= ["matched 2 params" 200]
             (my-router {:headers {:method "GET" :path "/file1/rob"}}))
    (should= ["matched 3 params" 200]
             (my-router {:headers {:method "GET" :path "/file1/rob/42"}})))

  (it "should match a path with a keyword param and have access to the params hash"
    (defrouter my-router [request params]
      (GET "/" ["root" 200])
      (GET "/:file" [(str "File = " (:file params))  200]))
    (should= ["File = file1" 200]
             (my-router {:headers {:method "GET" :path "/file1"}}))
    (should= ["File = file2" 200]
             (my-router {:headers {:method "GET" :path "/file2"}})))

  (it "should give a 404 error if no path matches"
    (defrouter my-router [request params]
      (GET "/" ["root" 200]))
    (should= 404 (second 
                   (my-router
                     {:headers {:method "GET" :path "/foo"}})))
    (should= java.io.StringBufferInputStream
             (class
               (:content-stream
                 (first 
                   (my-router
                     {:headers {:method "GET" :path "/foo"}}))))))

  (it "should give a 405 error if method not allowed"
    (defrouter my-router [request params]
      (GET "/foo" ["get foo" 200])
      (POST "/foo" ["post foo" 200]))
    (should= [{:headers {:Accept "GET, POST"}} 405]
             (my-router {:headers {:method "PUT" :path "/foo"}})))

  (it "should give a 405 error if method not allowed on params matches"
    (defrouter my-router [request params]
      (GET "/:foo" ["get foo" 200])
      (POST "/:foo" ["post foo" 200]))
    (should= [{:headers {:Accept "GET, POST"}} 405]
             (my-router {:headers {:method "PUT" :path "/file"}})))
)
