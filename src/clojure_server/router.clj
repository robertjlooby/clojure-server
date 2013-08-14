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

(defn params-match [router-path request-path]
  (loop [router-path-vec (parse-router-path router-path)
         request-path-vec (parse-path request-path)
         params {}]
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
        nil)))

(defmacro defrouter [router-name args & routes]
  `(defn ~router-name [~(first args)]
     ~(concat
       (list* 'cond 
            (apply concat
              (map #(list `(and (= ~(str (first %)) 
                                    (:method ~(first args)))
                                (params-match ~(second %) 
                                    (:path ~(first args))))
                           `(let [~(second args)
                                     (params-match
                                       ~(second %)
                                       (:path ~(first args)))]
                             ~(last %)))
                   routes)))
       '(:else ["Not Found" 404]))))
