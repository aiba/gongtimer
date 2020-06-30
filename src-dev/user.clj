(ns user
  (:require [clojure.tools.namespace.repl :as tnr]
            [mount.core :as mount]))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn post-reload []
  (start))

(defn reset []
  (println "reseting...")
  (stop)
  (time (tnr/refresh :after 'user/post-reload)))

(defn hard-reset []
  (tnr/clear)
  (time (reset)))
