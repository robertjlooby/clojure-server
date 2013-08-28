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

(defmacro route-functionizer [route-form sym1 sym2]
  `(fn [method# path#]
     (let [path-matches# (params-match ~(second route-form)
                                       path#)
           route-method# (str '~(first route-form))]
       (if (and (= method# route-method#) path-matches#)
         (partial (form-functionizer
                    ~(last route-form) ~sym2 ~sym1)
                  path-matches#)))))

(defmacro route-error-functionizer [route-form]
  `(fn [path#]
     (if (params-match ~(second route-form) path#)
       (str '~(first route-form)))))

(defn fnlist-to-fn [fnlist]
  (fn [method path]
    ((apply some-fn
      (map #(fn [v] (apply % v)) fnlist)) [method path])))

(defn fnlist-to-error-fn [fnlist]
  (fn [path]
    (remove nil?
            ((apply juxt fnlist) path))))

(defmacro routes-to-fns 
  ([sym1 sym2]
   [])
  ([sym1 sym2 router-route]
   `[(route-functionizer ~router-route ~sym1 ~sym2)])
  ([sym1 sym2 router-route & more-routes]
   `(concat
     [(route-functionizer ~router-route ~sym1 ~sym2)]
     (routes-to-fns ~sym1 ~sym2 ~@more-routes))))

(defmacro routes-to-error-fns
  ([]
   [])
  ([router-route]
   `[(route-error-functionizer ~router-route)])
  ([router-route & more-routes]
   `(concat
      [(route-error-functionizer ~router-route)]
      (routes-to-error-fns ~@more-routes))))

(defn error-response [accept]
 (if (seq accept)
    [{:headers {:Accept (clojure.string/join ", " accept)}} 405]
    [{:headers {:Content-Length 9}
      :content-stream
        (java.io.StringBufferInputStream.
          "Not Found")} 404]))

(defmacro routes-to-router-fn [sym1 sym2 & routes]
  `(fn [method# path#]
     ((fnlist-to-fn
        (routes-to-fns ~sym1 ~sym2 ~@routes)) method# path#)))

(defmacro defrouter [router-name args & routes]
  `(defn ~router-name [~(first args)]
     (let [request# ~(first args)
           request-path#   (:path   (:headers request#))
           request-method# (:method (:headers request#))
           router-fn# 
            ((routes-to-router-fn request# ~(second args) ~@routes)
               request-method# request-path#)
           error-fn# (fnlist-to-error-fn
                       (routes-to-error-fns ~@routes))]
       (if (fn? router-fn#)
         (router-fn# request#)
         (error-response (error-fn# request-path#))))))
