(ns clojure_server.router)

(defmacro defrouter [router-name args & routes]
  `(defn ~router-name [~(first args)]
     ~(list* 'cond 
            (apply concat
              (map #(list `(and (= ~(str (first %)) 
                                    (:method ~(first args)))
                                (= ~(second %) 
                                    (:path ~(first args))))
                           (last %))
                   routes)))))
