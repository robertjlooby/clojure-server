(ns clojure_server.router
  (:require [clojure.walk :refer [walk]]))

(defn parse-path [path]
  (let [relative-path (if (= \/ (first path))
                        (subs path 1)
                        path)]
    (clojure.string/split relative-path #"/")))

(defn parse-router-path [router-path]
  (let [path-vec (parse-path router-path)]
    (map #(if (= \: (first %))
           (keyword (subs % 1))
           %) path-vec)))

(defn decode-url [s]
  (java.net.URLDecoder/decode s))

(defn parse-request-path [request-path]
  (let [rel-path-vec (parse-path request-path)
        base-path (butlast rel-path-vec)
        [path-end query] (clojure.string/split 
                               (last rel-path-vec) #"\?" 2)
        params (if query
                 (->> query
                      (re-seq #"([^&]*)=([^&]*)")
                      (map rest)
                      (map #(walk decode-url vec %))
                      (map #(vector (keyword (first %)) (second %)))
                      (into {}))
                 {})]
    [(concat base-path [path-end]) params]))

(defn params-match [router-path request-path]
  (let [[request-path-vec params] (parse-request-path request-path)]
  (loop [router-path-vec (parse-router-path router-path)
         request-path-vec request-path-vec
         params params]
    (cond
      (not= (count router-path-vec) (count request-path-vec))
        nil
      (and (empty? router-path-vec) (empty? request-path-vec))
        params
      (keyword? (first router-path-vec))
        (recur (rest router-path-vec)
               (rest request-path-vec)
               (assoc params (first router-path-vec)
                             (first request-path-vec)))
      (= (first router-path-vec) (first request-path-vec))
        (recur (rest router-path-vec)
               (rest request-path-vec)
               params)
      :else
        nil))))

(defmacro form-functionizer [form & syms]
  `(fn [~@syms] ~form))

(defmacro forms-to-fns
  ([request-sym params-sym]
   [])
  ([request-sym params-sym form]
   `[(form-functionizer ~form ~request-sym ~params-sym)])
  ([request-sym params-sym form & more-forms]
   `(concat
     [(form-functionizer ~form ~request-sym ~params-sym)]
      (forms-to-fns ~request-sym ~params-sym ~@more-forms))))

(defmacro route-functionizer [route-form request-sym params-sym]
  `(fn [request#]
     (let [path#         (:path (:headers request#))
           method#       (:method (:headers request#))
           params#       (params-match ~(second route-form) path#)
           route-method# (str '~(first route-form))
           route-fns#    (forms-to-fns ~request-sym ~params-sym
                                       ~@(drop 2 route-form))]
       (if (and (= method# route-method#) params#)
         (walk #(% request# params#) last route-fns#)))))

(defmacro route-error-functionizer [route-form]
  `(fn [request#]
     (let [path# (:path (:headers request#))]
       (if (params-match ~(second route-form) path#)
         (str '~(first route-form))))))

(defn fnlist-to-fn [fnlist]
  (apply some-fn fnlist))

(defn fnlist-to-error-fn [fnlist]
  (fn [request]
    (remove nil?
            ((apply juxt fnlist) request))))

(defmacro routes-to-fns 
  ([request-sym params-sym]
   [])
  ([request-sym params-sym router-route]
   `[(route-functionizer ~router-route ~request-sym ~params-sym)])
  ([request-sym params-sym router-route & more-routes]
   `(concat
     [(route-functionizer ~router-route ~request-sym ~params-sym)]
     (routes-to-fns ~request-sym ~params-sym ~@more-routes))))

(defmacro routes-to-error-fns
  ([]
   [])
  ([router-route]
   `[(route-error-functionizer ~router-route)])
  ([router-route & more-routes]
   `(concat
      [(route-error-functionizer ~router-route)]
      (routes-to-error-fns ~@more-routes))))

(defn error-response [allow]
 (if (seq allow)
    [{:headers {:Allow (clojure.string/join
                         ", " (apply sorted-set allow))}} 405]
    [{:headers {:Content-Length 9}
      :content-stream
        (java.io.StringBufferInputStream.
          "Not Found")} 404]))

(defmacro routes-to-router-fn [request-sym params-sym & routes]
  `(fn [request#]
     (let [success-fn# 
              (fnlist-to-fn
                 (routes-to-fns ~request-sym ~params-sym ~@routes))
           error-fn# (fnlist-to-error-fn
                       (routes-to-error-fns ~@routes))]
       (or
         (success-fn# request#)
         (error-response (error-fn# request#))))))

(defmacro defrouter [router-name args & routes]
  `(defn ~router-name [~(first args)]
     (let [request# ~(first args)
           router-fn# 
            (routes-to-router-fn request# ~(second args) ~@routes)]
       (router-fn# request#))))
