(ns clojure_server.router)

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

(defn parse-request-path [request-path]
  (let [rel-path-vec (parse-path request-path)
        base-path (butlast rel-path-vec)
        [path-end query] (clojure.string/split 
                               (last rel-path-vec) #"\?" 2)
        params (if query
                 (into {} (map
                            #(vec 
                               (map 
                                 (fn [s]
                                   (java.net.URLDecoder/decode s))
                                 (rest %)))
                                (re-seq #"([^&]*)=([^&]*)" query)))
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

(defmacro defrouter [router-name args & routes]
  (let [accept (gensym)]
  `(defn ~router-name [~(first args)]
     (let [~accept (atom [])]
       ~(concat
         (list* 'cond 
              (apply concat
                (map #(list `(and (if (params-match ~(second %) (:path (:headers ~(first args))))
                                    (swap! ~accept conj ~(str (first %))))
                                  (= ~(str (first %)) 
                                      (:method (:headers ~(first args)))))
                             `(let [~(second args)
                                       (params-match
                                         ~(second %)
                                         (:path (:headers ~(first args))))]
                               ~(last %)))
                     routes)))
         `(:else 
             (if (seq @~accept)
                [{:headers {:Accept (clojure.string/join ", " @~accept)}} 405]
                ['("Not Found") 404])))))))
