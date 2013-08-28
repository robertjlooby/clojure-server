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

  (it "should return [['file'] {:a '123'}] for '/file?a=123'"
    (should= [["file"] {:a "123"}]
             (parse-request-path "/file?a=123")))

  (it "should return [['file' 'path' 'user'] {:name 'rob' :id '5'}] for '/file/path/user?name=rob&id=5'"
    (should= [["file" "path" "user"] {:name "rob", :id "5"}]
             (parse-request-path "/file/path/user?name=rob&id=5")))

  (it "should reutrn [['parameters'] {:var_1 'Operators <, >':var_2 'stuff'}] for '/parameters?var_1=Operators%20%3C%2C%20%3E&var_2=stuff'"
    (should= [["parameters"] {:var_1 "Operators <, >":var_2 "stuff"}]
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
    (should= {:user "rob" :id "123" :lang "clojure"}
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

(describe "route-functionizer"
  (it "should return a function"
    (should= true (fn? (route-functionizer (GET "/" "hello")
                                           request params))))

  (it "should return last form element as a fn if first 2 match"
    (let [fun (route-functionizer (GET "/" true) request params)
          fun2 (fun "GET" "/")]
      (should= true (fn? fun2))
      (should= true (fun2 nil))))

  (it "should match routes with params match"
    (let [fun (route-functionizer (PUT "/path/:file/:user" "hello")
                                  request params)
          fun2 (fun "PUT" "/path/file1/rob")]
      (should= "hello" (fun2 nil))))

  (it "should return nil if path does not match"
    (let [fun (route-functionizer (PUT "/path/:file/:user" "hello")
                                  request params)
          fun2 (fun "PUT" "/path2/file1/rob")]
      (should= nil fun2)))

  (it "should return nil if the path matches but not the method"
    (let [fun (route-functionizer (PUT "/path/:file/:user" "hello")
                                  request params)
          fun2 (fun "GET" "/path/file1/rob")]
      (should= nil fun2)))

  (it "should evaluate last element of form if matches"
    (let [fun (route-functionizer (GET "/path/:file/:user" (+ 2 2))
                                  request params)
          fun2 (fun "GET" "/path/file1/rob")]
      (should= 4 (fun2 {}))))

  (it "should have access to request and params in last
      element of form, with params partially applied"
    (let [fun (route-functionizer (GET "/path/:file/:user" 
                                       (str request (:user params)
                                                    (:var params)))
                                  request params)
          fun2 (fun "GET" "/path/file1/rob?var=yeah")]
      (should= "hey robyeah" (fun2 "hey "))))
)

(describe "route-error-functionizer"
  (it "should return a function"
    (should= true (fn? (route-error-functionizer (GET "/" "hello")))))

  (it "should return a fn that takes a path and returns nil if the 
       path does not match"
    (let [fun (route-error-functionizer (GET "/" true))]
      (should= nil (fun "/badpath"))))

  (it "should return a fn that takes a path and returns the route
       method if the path does match"
    (let [fun (route-error-functionizer (GET "/" true))]
      (should= "GET" (fun "/"))))

  (it "should return a fn that also matches params"
    (let [fun (route-error-functionizer (PUT "/url/:file" "hey"))]
      (should= "PUT" (fun "/url/index.html"))
      (should= "PUT" (fun "/url/hey?id=234&name=rob"))
      (should=  nil  (fun "/url/what/tooloong"))))
)

(describe "fnlist-to-fn"
  (it "should return a function that takes the method and path,
      returns falsy if no function returns truthy"
    (let [f (fnlist-to-fn [(fn [a b] (= a b)) (fn [a b] (= a b))])]
      (should= true (fn? f))
      (should-not (f "GET" "/"))))

  (it "should return a function that returns the return value of
       the first function that returns truthy"
    (let [f1 (fn [a b] (if (and (= a "GET") (= b "/")) "hey"))
          f2 (fn [a b] (if (= a "PUT") ["vec" "result"]))
          f (fnlist-to-fn [f1 f2])]
      (should= "hey" (f "GET" "/"))
      (should= ["vec" "result"] (f "PUT" "arg"))
      (should-not (f "GET" "/hey"))))

  (it "should consider a fn to be a truthy value and return it"
    (let [rf (fn [a b] (str a b))
          f1 (fn [a b] (if (and (= a "GET") (= b "/")) rf))
          f2 (fn [a b] (if (= a "PUT") ["vec" "result"]))
          f (fnlist-to-fn [f1 f2])]
      (should= rf (f "GET" "/"))
      (should= "hello joe" ((f "GET" "/") "hello " "joe"))))
)

(describe "fnlist-to-error-fn"
  (it "should return a fn that takes a path, returns [] if no fns
       in list return non-nil"
    (let [f (fnlist-to-error-fn [(fn [p] nil) (fn [p] nil)])]
      (should= [] (f "/"))))

  (it "should return an array of all non-nil return values"
    (let [f (fnlist-to-error-fn [(fn [p] nil)
                                 (fn [p] (if (= p "/") "GET"))
                                 (fn [p] (if (= p "/file") "POST"))
                                 (fn [p] (if (= p "/") "PUT"))])]
      (should= [] (f "/bad"))
      (should= ["GET" "PUT"] (f "/"))
      (should= ["POST"] (f "/file"))))
)

(describe "routes-to-fns"
  (it "should return [] for just var names"
    (should= [] (routes-to-fns request params)))

  (it "should return a function from the route for 1 route"
    (let [fns (routes-to-fns request params (GET "/" "hello"))
          fn1 ((first fns) "GET" "/")]
      (should= true (fn? (first fns)))
      (should= true (fn? fn1))
      (should= "hello" (fn1 {}))))

  (it "should return a function that matches based on params"
    (let [fns (routes-to-fns request params (GET "/:file" "hello"))
          fn1 ((first fns) "GET" "/file1")]
      (should= true (fn? (first fns)))
      (should= true (fn? fn1))
      (should= "hello" (fn1 {}))))

  (it "should return a function with access to the var names 
       from the route for 1 route"
    (let [fns (routes-to-fns v1 v2 (GET "/:word" (str v1 (:word v2))))
          fn1 ((first fns) "GET" "/world")]
      (should= true (fn? (first fns)))
      (should= true (fn? fn1))
      (should= "hello world" (fn1 "hello "))))

  (it "should return a list of functions for >1 routes"
    (let [fns (routes-to-fns v1 v2 (GET "/" "GET root")
                                   (PUT "/f1" "PUT f1")
                                   (POST "/f2" "POST f2"))
          fn2 ((second fns) "PUT" "/f1")]
      (should= 3 (count fns))
      (should= "PUT f1" (fn2 {}))))
)

(describe "routes-to-error-fns"
  (it "should return [] for no args"
    (should= [] (routes-to-error-fns)))

  (it "should return a list of one function for one route"
    (let [fns (routes-to-error-fns (GET "/" "hey"))
          fn1 (first fns)]
      (should= true (fn? fn1))
      (should= "GET" (fn1 "/"))
      (should=  nil  (fn1 "/bad"))))

  (it "should return a list of n functions for n routes"
    (let [fns (routes-to-error-fns (GET "/" "hey")
                                   (PUT "/" "yo")
                                   (GET "/file" "file"))
          fn1 (first fns)
          fn2 (second fns)
          fn3 (last fns)]
      (should= 3 (count fns))
      (should= "GET" (fn1 "/"))
      (should= "PUT" (fn2 "/"))
      (should= "GET" (fn3 "/file"))
      (should= '(nil nil nil) (map #(% "/bad") fns))))
)

(describe "routes-to-router-fn"
  (it "returns a fn that takes the method and path,
       returns a fn of the result of the matching path"
    (let [f (routes-to-router-fn v1 v2 (GET "/" "got root")
                                       (PUT "/:form" (:form v2)))]
      (should= true (fn? f))
      (should= true (fn? (f "GET" "/")))
      (should= "got root" ((f "GET" "/") {}))))

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
