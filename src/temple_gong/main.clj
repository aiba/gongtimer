(ns temple-gong.main
  (:require [temple-gong.gong :as gong]))

(defn ^:private sleep-rand-mins [a b]
  (let [x (+ a (* (rand) (- b a)))]
    (println (format "sleeping for %.2f minutes" x))
    (Thread/sleep (int (* 1000 60 x)))))

(defn -main [& args]
  #_(loop []
    (gong!)
    (sleep-rand-mins 1 7)
    (recur)))

(comment
  (day-time?)
  (gong!)
  )
