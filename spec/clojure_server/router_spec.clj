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

(describe "parse-query-to-params"
  (it "should return an empty map for nil"
    (should= {} (parse-query-to-params nil)))

  (it "should return an empty map for an empty string"
    (should= {} (parse-query-to-params "")))

  (it "should return {:a "123" :b 'abc'} for a=123&b=abc"
    (should= {:a "123" :b "abc"}
             (parse-query-to-params "a=123&b=abc")))

  (it "should return {:var_1 'Operators <, >' :var_2 'stuff'} for
       var_1=Operators%20%3C%2C%20%3E&var_2=stuff"
    (should= {:var_1 "Operators <, >" :var_2 "stuff"}
             (parse-query-to-params
               "var_1=Operators%20%3C%2C%20%3E&var_2=stuff")))
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

  (it "should reutrn [['parameters'] {:var_1 'Operators <, >' :var_2 'stuff'}] for '/parameters?var_1=Operators%20%3C%2C%20%3E&var_2=stuff'"
    (should= [["parameters"] {:var_1 "Operators <, >" :var_2 "stuff"}]
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

  (it "should return nil with a regex path when not a match"
    (should= nil (params-match #"/([a-z]+)" "/1234")))

  (it "should return {0 '/'} for a match of #'/' to '/'"
    (should= {0 "/"} (params-match #"/" "/")))

  (it "should return {0 '/rob/123' 1 'rob' 2 '123'} for a match of
       #'/([a-z]+)/(\\d+)' to '/rob/123'"
    (should= {0 "/rob/123" 1 "rob" 2 "123"} 
             (params-match #"/([a-z]+)/(\d+)" "/rob/123")))

  (it "should return {0 '/a/1/file' 1 'a' 2 '1' :v '123' :x '<, >'}
       for a match of #'/([a-z])/([0-9])/file' 
       to '/a/1/file?v=123&x=%3C%2C%20%3E'"
    (should= {0 "/a/1/file" 1 "a" 2 "1" :v "123" :x "<, >"}
             (params-match #"/([a-z])/([0-9])/file"
                           "/a/1/file?v=123&x=%3C%2C%20%3E")))

  (it "should return {0 '/a/1/file' :v '123' :x '<, >'}
       for a match of #'/[a-z]/[0-9]/file' 
       to '/a/1/file?v=123&x=%3C%2C%20%3E'"
    (should= {0 "/a/1/file" :v "123" :x "<, >"}
             (params-match #"/[a-z]/[0-9]/file"
                           "/a/1/file?v=123&x=%3C%2C%20%3E")))

  (it "should return nil for a match of
       #'/([a-z]+)/(\\d+)/pa' to '/rob/123/path'"
    (should= nil 
             (params-match #"/([a-z]+)/(\d+)/pa" "/rob/123/path")))
)

(describe "form-functionizer"
  (it "should return a string unmodified"
    (let [f "hello"
          fun (form-functionizer arg arg2 f)]
      (should= "hello" (fun nil nil))))

  (it "should evaluate a function"
    (let [fun (form-functionizer arg arg2 (str "Hello " "World"))]
      (should= "Hello World" (fun nil nil))))

  (it "should have access to first arg"
    (let [fun (form-functionizer nme arg2 (str "Hello " nme))]
      (should= "Hello Rob" (fun "Rob" nil))))

  (it "should have access to second arg"
    (let [fun (form-functionizer arg1 a (+ a 5))]
      (should= 12 (fun nil 7))))

  (it "should have access to both args"
    (let [fun (form-functionizer arg1 arg2 (zipmap arg1 arg2))]
      (should= {:a 1 :b 2 :c 3} (fun [:a :b :c] [1 2 3]))))
)

(describe "route-functionizer"
  (it "should return a function"
    (should= true (fn? (route-functionizer (GET "/" "hello")
                                           request params))))

  (it "should return last form element as a fn if first 2 match"
    (let [fun (route-functionizer (GET "/" true) request params)
          req {:headers {:method "GET" :path "/"}}]
      (should= true (fun req))))

  (it "should match routes with params match"
    (let [fun (route-functionizer (PUT "/path/:file/:user" "hello")
                                  request params)
          req {:headers {:method "PUT" :path "/path/file1/rob"}}]
      (should= "hello" (fun req))))

  (it "should return nil if path does not match"
    (let [fun (route-functionizer (PUT "/path/:file/:user" "hello")
                                  request params)
          req {:headers {:method "PUT" :path "/path2/file1/rob"}}]
      (should= nil (fun req))))

  (it "should return nil if the path matches but not the method"
    (let [fun (route-functionizer (PUT "/path/:file/:user" "hello")
                                  request params)
          req {:headers {:method "GET" :path "/path/file1/rob"}}]
      (should= nil (fun req))))

  (it "should evaluate last element of form if matches"
    (let [fun (route-functionizer (GET "/path/:file/:user" (+ 2 2))
                                  request params)
          req {:headers {:method "GET" :path "/path/file1/rob"}}]
      (should= 4 (fun req))))

  (it "should have access to request and params in last
      element of form, with params partially applied"
    (let [fun (route-functionizer (GET "/path/:file/:user" 
                                       (str (:method 
                                              (:headers request))
                                            (:user params)
                                            (:var params)))
                                  request params)
          req {:headers
               {:method "GET" :path "/path/file1/rob?var=yeah"}}]
      (should= "GETrobyeah" (fun req))))

  (it "should be able to have multiple forms as the route body
       and get the result of the last one as the return value"
    (let [a (atom 0)
          b (atom 5)
          c (atom 10)
          fun (route-functionizer (PUT "/path/:file/:user"
                                       (swap! a inc)
                                       (swap! b dec)
                                       (reset! c "hey")
                                       @c)
                                  request params)
          req {:headers {:method "PUT" :path "/path/file1/rob"}}
          result (fun req)]
      (should= "hey" result)
      (should= 1 @a)
      (should= 4 @b)
      (should= "hey" @c)))

  (it "should be able to have multiple forms as the route body
       and access to request/params in all of them"
    (let [a (atom 0)
          b (atom 5)
          fun (route-functionizer (PUT "/path/:file/:user"
                                     (reset! a (:method
                                                (:headers request)))
                                     (reset! b (:file params))
                                     (str (:path (:headers request))
                                          (:user params)))

                                  request params)
          req {:headers {:method "PUT" :path "/path/file1/rob"}}
          result (fun req)]
      (should= "/path/file1/robrob" result)
      (should= "PUT" @a)
      (should= "file1" @b)))
)

(describe "route-error-functionizer"
  (it "should return a function"
    (should= true (fn? (route-error-functionizer (GET "/" "hello")))))

  (it "should return a fn that takes a request and returns nil
       if the path does not match"
    (let [fun (route-error-functionizer (GET "/" true))]
      (should= nil (fun {:headers {:path "/badpath"}}))))

  (it "should return a fn that takes a request and returns the route
       method if the path does match"
    (let [fun (route-error-functionizer (GET "/" true))]
      (should= "GET" (fun {:headers {:path "/"}}))))

  (it "should return a fn that also matches params"
    (let [fun (route-error-functionizer (PUT "/url/:file" "hey"))]
      (should= "PUT" (fun {:headers {:path "/url/index.html"}}))
      (should= "PUT" (fun {:headers 
                           {:path "/url/hey?id=234&name=rob"}}))
      (should=  nil  (fun {:headers 
                           {:path "/url/what/tooloong"}}))))
)

(describe "fnlist-to-fn"
  (it "should return a function that takes the request,
      returns falsy if no function returns truthy"
    (let [f (fnlist-to-fn [(fn [r] (= "a" r)) (fn [r] (= "b" r))])]
      (should= true (fn? f))
      (should-not (f "/"))))

  (it "should return a function that returns the return value of
       the first function that returns truthy"
    (let [f1 (fn [r] (if (= r "GET") "hey"))
          f2 (fn [r] (if (= r "PUT") ["vec" "result"]))
          f (fnlist-to-fn [f1 f2])]
      (should= "hey" (f "GET"))
      (should= ["vec" "result"] (f "PUT"))
      (should-not (f "/hey"))))
)

(describe "fnlist-to-error-fn"
  (it "should return a fn that takes a request, returns [] if no fns
       in list return non-nil"
    (let [f (fnlist-to-error-fn [(fn [r] nil) (fn [r] nil)])]
      (should= [] (f "/"))))

  (it "should return an array of all non-nil return values"
    (let [f (fnlist-to-error-fn [(fn [r] nil)
                                 (fn [r] (if (= r "/") "GET"))
                                 (fn [r] (if (= r "/file") "POST"))
                                 (fn [r] (if (= r "/") "PUT"))])]
      (should= [] (f "/bad"))
      (should= ["GET" "PUT"] (f "/"))
      (should= ["POST"] (f "/file"))))
)

(describe "routes-to-fns"
  (it "should return [] for just var names"
    (should= [] (routes-to-fns request params)))

  (it "should return a function from the route for 1 route"
    (let [fns (routes-to-fns request params (GET "/" "hello"))
          req {:headers {:method "GET" :path "/"}}]
      (should= true (fn? (first fns)))
      (should= "hello" ((first fns) req))))

  (it "should return a function that matches based on params"
    (let [fns (routes-to-fns request params (GET "/:file" "hello"))
          req {:headers {:method "GET" :path "/file1"}}]
      (should= "hello" ((first fns) req))))

  (it "should return a function with access to the var names 
       from the route for 1 route"
    (let [fns (routes-to-fns v1 v2 (GET "/:word" (str (:method
                                                        (:headers v1))
                                                      (:word v2))))
          req {:headers {:method "GET" :path "/world"}}]
      (should= "GETworld" ((first fns) req))))

  (it "should return a list of functions for >1 routes"
    (let [fns (routes-to-fns v1 v2 (GET "/" "GET root")
                                   (PUT "/f1" "PUT f1")
                                   (POST "/f2" "POST f2"))
          req {:headers {:method "PUT" :path "/f1"}}]
      (should= 3 (count fns))
      (should= "PUT f1" ((second fns) req))))
)

(describe "routes-to-error-fns"
  (it "should return [] for no args"
    (should= [] (routes-to-error-fns)))

  (it "should return a list of one function for one route"
    (let [fns (routes-to-error-fns (GET "/" "hey"))]
      (should= true (fn? (first fns)))
      (should= "GET" ((first fns) {:headers {:path "/"}}))
      (should=  nil  ((first fns) {:headers {:path "/bad"}}))))

  (it "should return a list of n functions for n routes"
    (let [fns (routes-to-error-fns (GET "/" "hey")
                                   (PUT "/" "yo")
                                   (GET "/file" "file"))
          fn1 (first fns)
          fn2 (second fns)
          fn3 (last fns)]
      (should= 3 (count fns))
      (should= "GET" (fn1 {:headers {:path "/"}}))
      (should= "PUT" (fn2 {:headers {:path "/"}}))
      (should= "GET" (fn3 {:headers {:path "/file"}}))
      (should= '(nil nil nil) (map #(% {:headers {:path "/bad"}}) 
                                   fns))))
)

(describe "error-response"
  (it "should return a 404 if passed []"
    (should= 404 (second (error-response []))))

  (it "should return a 405 with Allow header containing methods"
    (let [resp (error-response ["POST" "GET" "PUT" "GET" "DELETE"])]
      (should= 405 (second resp))
      (should= "DELETE, GET, POST, PUT"
               (:Allow (:headers (first resp))))))
)

(describe "routes-to-router-fn"
  (it "returns a fn that takes the request,
       returns the result of the matching path"
    (let [f (routes-to-router-fn v1 v2 (GET "/" "got root")
                                       (PUT "/:form" (:form v2))
                                       (POST "/:form" 
                                             (str (:method 
                                                    (:headers v1))
                                                  (:form v2))))]
      (should= true (fn? f))
      (should= "got root" (f {:headers
                              {:method "GET" :path "/"}}))
      (should= "POSTfile1" (f {:headers
                              {:method "POST" :path "/file1"}}))))

  (it "returns a fn that takes the request,
       returns an error if no path matches"
    (let [f (routes-to-router-fn v1 v2 (GET "/" "got root")
                                       (PUT "/:form" (:form v2))
                                       (POST "/:form" 
                                             (str (:method 
                                                    (:headers v1))
                                                  (:form v2))))]
      (should= 404 (second (f {:headers
                              {:method "POST" :path "/f1/bad"}})))
      (should= 405 (second (f {:headers
                              {:method "GET" :path "/f1"}})))
      (should= "POST, PUT"
              (:Allow (:headers
                         (first (f {:headers
                              {:method "GET" :path "/f1"}})))))))

  (it "only includes each method once in 405 Allow header"
    (let [f (routes-to-router-fn v1 v2 (GET "/" "got root")
                                       (PUT "/f1" "put f1")
                                       (PUT "/:form" (:form v2))
                                       (POST "/:form" 
                                             (str (:method 
                                                    (:headers v1))
                                                  (:form v2))))]
      (should= 405 (second (f {:headers
                              {:method "GET" :path "/f1"}})))
      (should= "POST, PUT"
              (:Allow (:headers
                         (first (f {:headers
                              {:method "GET" :path "/f1"}})))))))
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
    (should= [{:headers {:Allow "GET, POST"}} 405]
             (my-router {:headers {:method "PUT" :path "/foo"}})))

  (it "should give a 405 error if method not allowed on params matches"
    (defrouter my-router [request params]
      (GET "/:foo" ["get foo" 200])
      (POST "/:foo" ["post foo" 200]))
    (should= [{:headers {:Allow "GET, POST"}} 405]
             (my-router {:headers {:method "PUT" :path "/file"}})))
)
