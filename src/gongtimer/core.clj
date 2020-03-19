(ns gongtimer.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import com.sun.javafx.application.PlatformImpl
           com.sun.media.sound.DirectAudioDeviceProvider
           javax.sound.sampled.Mixer$Info
           javax.sound.sampled.Mixer
           java.net.URL
           [javafx.scene.media Media MediaPlayer]
           javax.sound.sampled.SourceDataLine
           javax.sound.sampled.Line
           javax.sound.sampled.Line$Info
           javax.sound.sampled.AudioSystem))

(def url->bytes
   (memoize
    (fn [^URL url]
      (with-open [xin (io/input-stream url)
                  xout (java.io.ByteArrayOutputStream.)]
        (io/copy xin xout)
        (.toByteArray xout)))))

(defn start-jfx! []
  @(let [p (promise)]
     (PlatformImpl/startup (fn []
                             (deliver p true)))
     p))

(defn output-mixers []
  (let [output-line (Line$Info. SourceDataLine)]
    (->> (AudioSystem/getMixerInfo)
         (filter (fn [^Mixer$Info mi]
                   (-> mi
                       AudioSystem/getMixer
                       (.isLineSupported output-line)))))))

(defn play-audio-url! [^URL url, ^Mixer$Info mixer-info]
  (let [s (-> url url->bytes io/input-stream AudioSystem/getAudioInputStream)
        ;;m (AudioSystem/getMixer mixer-info)
        ]
    (future
      (doto (AudioSystem/getClip mixer-info) (.open s) (.start)))))



(comment
  (start-jfx!)

  (str (second (output-mixers)))

  (play-audio-url! (io/resource "gong-chinese-1.wav")
                   (last (output-mixers)))

  )



#_(defn slurp-bytes [path]
   (url->bytes (io/resource path)))

(comment
  (url->bytes (io/resource "gong-low-eerie-1_G1gkOGV__NWM.mp3"))
  1
  )

(defn play-audio-url! [^URL url]
  (let [s (-> url url->bytes io/input-stream AudioSystem/getAudioInputStream)]
    (future
      (doto (AudioSystem/getClip) (.open s) (.start)))))

(comment

  (play-audio-url! (io/resource "gong-low-eerie-1_G1gkOGV__NWM.mp3"))

  )
