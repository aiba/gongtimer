(ns temple-gong.main
  (:require [temple-gong.gong :as gong]
            [mount.core :as mount]))

(defn alog [& args]
  (apply println (if-let [id (:id @*agent*)]
                   (str "[" id "]")
                   "")
         args))

(defn ^:private sleep-rand-mins
  ([a b]
   (let [x (+ a (* (rand) (- b a)))]
     (alog (format "sleeping for %.2f minutes" x))
     (Thread/sleep (int (* 1000 60 x)))))
  ([]
   (sleep-rand-mins 1 7)
   ;;(sleep-rand-mins 0 1) ;; for debugging
   ))

;; Agent ———————————————————————————————————————————————————————————————————————————

(defn new-gong-agent []
  (doto (agent {:id (-> (System/currentTimeMillis)
                        (/ 100)
                        (Math/floor)
                        (long)
                        (mod (* 10 60 60 24)))
                :stop false
                :pause-until nil})
    (set-error-handler! (fn [a ^Throwable t]
                          (println "gong-agent error")
                          (.printStackTrace t)))))

(defn paused? [state]
  (when-let [ts (:pause-until state)]
    (< (System/currentTimeMillis) ts)))

(defn process! [state]
  (if (:stop state)
    (alog "stopping")
    (do (if (paused? state)
          (alog "(paused)")
          (gong/play!))
        (future
          (sleep-rand-mins)
          (send-off *agent* #'process!))))
  state)

(mount/defstate ^:private *gong-agent
  :start (doto (new-gong-agent)
           (send-off process!))
  :stop (send-off *gong-agent #(assoc % :stop true)))

(defn pause-until! [ts]
  (send *gong-agent #(assoc % :pause-until ts)))

(defn mins-in-future [n]
  (+ (System/currentTimeMillis)
     (* n 1000 60)))

;; Main ————————————————————————————————————————————————————————————————————————————

(defn -main [& args]
  (println "temple-gong.main")
  (mount/start))

(comment

  *gong-agent
  (pause-until! (mins-in-future 2))
  (paused? @*gong-agent)

  (send-off *gong-agent #(assoc % :stop true))

  (def a (new-gong-agent))
  @a


  (day-time?)
  (gong!)
  )
