(ns gongtimer.core
  (:require [clojure.java.io :as io])
  (:import java.net.URL
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
  (let [n (+ a (* (rand) (- b a)))]
    (println "sleeping for" n "minutes")
    (Thread/sleep (int (* 1000 60 n)))))

(defn -main [& args]
  (loop []
    (gong!)
    (sleep-rand-mins 1 7)
    (recur)))

(comment
  (gong!)
  )
