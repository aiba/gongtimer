(ns temple-gong.main
  (:require [temple-gong.gong :as gong]
            [temple-gong.tray :as tray]
            [mount.core :as mount]))

;; Main ————————————————————————————————————————————————————————————————————————————

(defn -main [& args]
  (println "temple-gong.main")
  (mount/start))
