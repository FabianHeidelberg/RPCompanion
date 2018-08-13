(ns rp-companion.viewer
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [cljsjs.simple-peer]))

(defonce viewer-peer (js/SimplePeer. #js {:initiator true :trickle false}))
(defn init-webrtc []
  (.on viewer-peer "error" (fn [] (println "Viewer error")))
  (.on viewer-peer "signal" (fn [data]
    (do
      (println "Signaled viewer")
      (rf/dispatch [:signal-master data]))))
      ;; signal master-peer here
      ;;)))
  (.on viewer-peer "connect" (fn [] (println "Connected viewer")))
  (.on viewer-peer "data" (fn [data] (println "data" data))))

;; Event handler
(rf/reg-event-db
  :viewer/initialize
  (fn [_ [_ room-id]]
    (do
      (init-webrtc)
      (println "Viewer" (js/JSON.stringify viewer-peer))
      {:start true})))

(defn main-view []
  [:div {}
    [:h1 "viewer" ]
    [:p "todo: implement"]])

(rf/reg-event-fx
  :signal-viewer
  (fn [incoming-effects [_ data]]
    (do
      (println "Viewer data" (js/JSON.stringify data))
      (.signal viewer-peer data))))
