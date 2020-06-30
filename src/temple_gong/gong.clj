(ns temple-gong.gong
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import java.net.URL
           java.time.LocalDateTime
           [javax.sound.sampled AudioSystem Clip FloatControl FloatControl$Type Line$Info Mixer Mixer$Info SourceDataLine]))

(set! *warn-on-reflection* true)

(def ^:private day-hours [9, 21])

(defn ^:private day-time? []
  (let [x (.getHour (LocalDateTime/now))
        [a b] day-hours]
    (and (<= a x)
         (<= x b))))

(def ^:private url->bytes
   (memoize
    (fn [^URL url]
      (with-open [xin (io/input-stream url)
                  xout (java.io.ByteArrayOutputStream.)]
        (io/copy xin xout)
        (.toByteArray xout)))))

(defn ^:private gain-config [mixer-name]
  (let [f (io/file "volume.edn")
        m (if (.exists f)
            (read-string (slurp f))
            {})
        m (update m mixer-name #(mapv double (or % [0 0])))
        [day night] (get m mixer-name)]
    (spit f (with-out-str (pprint m)))
    (if (day-time?) day night)))

(defn ^:private output-mixers []
  (let [output-line (Line$Info. SourceDataLine)]
    (->> (AudioSystem/getMixerInfo)
         (map (fn [^Mixer$Info mi]
                (let [mixer (AudioSystem/getMixer mi)]
                  {:name (.getName mi)
                   :mixer-info mi
                   :mixer mixer})))
         (filter (fn [{:keys [^Mixer mixer]}]
                   (.isLineSupported mixer output-line)))
         (remove (fn [{:keys [^Mixer$Info mixer-info]}]
                   (= "Default Audio Device" (.getName mixer-info)))))))

(defn ^:private set-gain! [^Clip clip, ^double x]
  (let [^FloatControl ctrl (.getControl clip FloatControl$Type/MASTER_GAIN)]
    (.setValue ctrl x)))

(defn ^:private get-clip [^URL url, ^Mixer$Info mixer-info, gain]
  (let [^AudioInputStream s (-> url url->bytes io/input-stream
                                AudioSystem/getAudioInputStream)]
    (doto (AudioSystem/getClip mixer-info)
      (.open s)
      (set-gain! gain))))

(def ^:private gong-sound-url (io/resource "gong.wav"))

(defn play! []
  (let [clips (doall
               (for [{:keys [name mixer-info]} (output-mixers)]
                 (get-clip gong-sound-url mixer-info (gain-config name))))]
    (doseq [^Clip c clips]
      (future (.start c)))))
