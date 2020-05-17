(ns temple-gong.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import java.net.URL
           java.time.LocalDateTime
           [javax.sound.sampled AudioSystem Clip FloatControl FloatControl$Type Line$Info Mixer$Info SourceDataLine]))

(def url->bytes
   (memoize
    (fn [^URL url]
      (with-open [xin (io/input-stream url)
                  xout (java.io.ByteArrayOutputStream.)]
        (io/copy xin xout)
        (.toByteArray xout)))))

(defn gain-config [mixer-name]
  (let [f (io/file "volume.edn")
        m (if (.exists f)
            (read-string (slurp f))
            {})
        m (update m mixer-name #(double (or % 0)))]
    (spit f (with-out-str (pprint m)))
    (get m mixer-name)))

(defn output-mixers []
  (let [output-line (Line$Info. SourceDataLine)]
    (->> (AudioSystem/getMixerInfo)
         (map (fn [^Mixer$Info mi]
                (let [mixer (AudioSystem/getMixer mi)]
                  {:name (.getName mi)
                   :mixer-info mi
                   :mixer mixer})))
         (filter (fn [{:keys [mixer]}]
                   (.isLineSupported mixer output-line)))
         (remove (fn [{:keys [mixer-info]}]
                   (= "Default Audio Device" (.getName mixer-info)))))))

(defn set-gain! [^Clip clip, ^double x]
  (-> clip
      (.getControl FloatControl$Type/MASTER_GAIN)
      (.setValue x)))

(defn get-clip [^URL url, ^Mixer$Info mixer-info, gain]
  (let [^AudioInputStream s (-> url url->bytes io/input-stream
                                AudioSystem/getAudioInputStream)]
    (doto (AudioSystem/getClip mixer-info)
      (.open s)
      (set-gain! gain))))

(def gong-sound-url (io/resource "gong.wav"))

(defn gong! []
  (let [clips (doall
               (for [{:keys [name mixer-info]} (output-mixers)]
                 (get-clip gong-sound-url mixer-info (gain-config name))))]
    (doseq [c clips]
      (future (.start c)))))

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
