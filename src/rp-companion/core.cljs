(ns rp-companion.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [clojure.string :as str]
            [rp-companion.master :as master]
            [rp-companion.viewer :as viewer]
            [rp-companion.lobby :as lobby])
  (:import [goog History]
           [goog.history EventType]))

`(devtools/install!)

(def app-node (js/document.getElementById "app"))

;; Define routes

(defroute "/" []
  (do
    (rf/dispatch-sync [:initialize])
    (reagent/render [lobby/main-view] app-node)))

(defroute "/master/:id" [id]
  (do
    (rf/dispatch-sync [:initialize id])
    (reagent/render [master/main-view] app-node)))

(defroute "/viewer/:id" [id]
  (do
    (rf/dispatch-sync [:initialize id])
    (reagent/render [viewer/main-view] app-node)))


;; Hookup routing

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
      (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn ^:export run
  [])