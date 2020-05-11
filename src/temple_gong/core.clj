(ns temple-gong.core
  (:require [clojure.java.io :as io])
  (:import java.net.URL
           java.time.LocalDateTime
           [javax.sound.sampled AudioSystem Line$Info Mixer$Info SourceDataLine]))

(def url->bytes
   (memoize
    (fn [^URL url]
      (with-open [xin (io/input-stream url)
                  xout (java.io.ByteArrayOutputStream.)]
        (io/copy xin xout)
        (.toByteArray xout)))))

(defn output-mixers []
  (let [output-line (Line$Info. SourceDataLine)]
    (->> (AudioSystem/getMixerInfo)
         (filter (fn [^Mixer$Info mi]
                   (-> mi
                       AudioSystem/getMixer
                       (.isLineSupported output-line))))
         (remove (fn [^Mixer$Info mi]
                   (= "Default Audio Device" (.getName mi)))))))

(defn play-audio-url! [^URL url, ^Mixer$Info mixer-info]
  (let [s (-> url url->bytes io/input-stream AudioSystem/getAudioInputStream)]
    (future
      (doto (AudioSystem/getClip mixer-info) (.open s) (.start)))))

(def gong-sound-url (io/resource "gong.wav"))

(defn gong! []
  (doseq [m (output-mixers)]
    (play-audio-url! gong-sound-url m)))

(defn sleep-rand-mins [a b]
  (let [x (+ a (* (rand) (- b a)))]
    (println (format "sleeping for %.2f minutes" x))
    (Thread/sleep (int (* 1000 60 x)))))

(def day-hours [9, 21])  ;; only play gong 9am-9pm

(defn day-time? []
  (let [x (.getHour (LocalDateTime/now))
        [a b] day-hours]
    (and (<= a x)
         (<= x b))))

(defn -main [& args]
  (loop []
    (when (day-time?)
      (gong!))
    (sleep-rand-mins 1 7)
    (recur)))

(comment
  (day-time?)
  (gong!)
  )
