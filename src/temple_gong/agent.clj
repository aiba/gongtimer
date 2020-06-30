(ns temple-gong.agent
  (:require [temple-gong.gong :as gong]
            [mount.core :as mount]))

(defn ^:private alog [& args]
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

(defn ^:private new-gong-agent []
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

(defn ^:private agent-paused? [state]
  (when-let [ts (:pause-until state)]
    (< (System/currentTimeMillis) ts)))

(defn ^:private process! [state]
  (if (:stop state)
    (alog "stopping")
    (do (if (agent-paused? state)
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

;; API —————————————————————————————————————————————————————————————————————————————

(defn paused? []
  (agent-paused? @*gong-agent))

(defn mins-in-future [n]
  (+ (System/currentTimeMillis)
     (* n 1000 60)))

(defn pause-for-mins! [n]
  (send *gong-agent #(assoc % :pause-until (mins-in-future n))))
