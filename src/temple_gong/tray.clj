(ns temple-gong.tray
  (:require [clojure.java.io :as io]
            [temple-gong.agent :as ga]
            [temple-gong.gong :as gong]
            [mount.core :as mount])
  (:import [java.awt Menu MenuItem PopupMenu SystemTray Toolkit TrayIcon]
           java.awt.event.ActionListener
           javax.swing.JOptionPane))

;; utils ———————————————————————————————————————————————————————————————————————————

(defn show-dialog [m]
  (JOptionPane/showMessageDialog nil m))

(defn system-tray ^SystemTray []
  (SystemTray/getSystemTray))

(defn remove-all-trays! []
  (let [tray (system-tray)]
    (doseq [i (.getTrayIcons tray)]
      (.remove tray i))))

;; menu rendering lib ——————————————————————————————————————————————————————————————

(defn ^:private action-listener [f]
  (proxy [ActionListener] []
    (actionPerformed [event]
      (when f
        (f event)))))

(defn ^:private flatten-seqs [s]
  (apply concat (for [x s]
                  (cond
                    (vector? x) [x]
                    (sequential? x) x
                    :else [x]))))

(defn generate ^MenuItem [[k opts & children]]
  (letfn [(add-children [^Menu m, children]
            (doseq [c (flatten-seqs children)]
              (assert (vector? c))
              (if (= (first c) :separator)
                (.addSeparator m)
                (.add m (generate c)))))]
    (case k
      :popup (doto (PopupMenu.)
               (add-children children))
      :menu (doto (Menu.)
              (.setLabel (get opts :label ""))
              (add-children children))
      :item (doto (MenuItem.)
              (.setLabel (get opts :label ""))
              (.setEnabled (get opts :enabled? true))
              (.addActionListener (action-listener (:on-click opts)))))))

;; gong menu ———————————————————————————————————————————————————————————————————————

(declare re-render!)

(defn render-menu []
  [:popup {}
   #_[:item {:label (if (ga/paused?)
                    "(Paused)"
                    "(Running)")}]
   [:separator]
   [:menu {:label "Pause for..."}
    (for [n [20 60 90 120]]
      [:item {:label (str n " minutes")
              :on-click (fn [_]
                          (ga/pause-for-mins! n)
                          #_(future (Thread/sleep 10)
                                  (re-render!)))}])]
   [:separator]
   [:item {:label "Quit"
           :on-click (fn [_] (System/exit 0))}]])

(defn render! [^TrayIcon ti]
  (.setPopupMenu ti (generate (render-menu))))

(defn new-tray-icon []
  (let [icon (doto (TrayIcon. (.getImage (Toolkit/getDefaultToolkit)
                                         (io/resource "noun_Gong_184096_cropped.png")))
               (.setImageAutoSize true))]
    (.add (system-tray) icon)
    (render! icon)
    icon))

(when (SystemTray/isSupported)
  (mount/defstate ^:private *icon
    :start (new-tray-icon)
    :stop (remove-all-trays!)))

(defn re-render! []
  (render! *icon))

(comment
  (remove-all-trays!)
  (new-tray-icon)
  )
