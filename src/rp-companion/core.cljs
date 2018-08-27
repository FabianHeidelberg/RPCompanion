(ns rp-companion.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [clojure.string :as str]
            [com.degel.re-frame-firebase :as firebase]
            [rp-companion.master :as master]
            [rp-companion.viewer :as viewer]
            [rp-companion.lobby :as lobby])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def app-node (js/document.getElementById "app"))

;; Define routes

(defroute "/" []
  (do
    (rf/dispatch-sync [:lobby/initialize])
    (reagent/render [lobby/main-view] app-node)))

(defroute "/master/:id" [id]
  (do
    (rf/dispatch-sync [:master/initialize id])
    (reagent/render [master/main-view] app-node)))

(defroute "/viewer/:id" [id]
  (do
    (rf/dispatch-sync [:viewer/initialize id])
    (reagent/render [viewer/main-view] app-node)))


;; Hookup routing

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
      (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; Setup firebase

(defonce firebase-app-info
  {:apiKey "AIzaSyDma65tAeli5ZbSRz7c3KbIyjSqaOyqUUE"
   :authDomain "rp-companion.firebaseapp.com"
   :databaseURL "https://rp-companion.firebaseio.com"
   :projectId "rp-companion"
   :storageBucket "rp-companion.appspot.com"
   :messagingSenderId "139282990817"})

(rf/reg-event-db :set-user (fn [db [_ user]]
  (println "setuser" user)
  (assoc db :user user)))

(rf/reg-sub :user (fn [db _] (:user db)))

(firebase/init
  :firebase-app-info firebase-app-info
  :firestore-settings {:timestampsInSnapshots true} ; See: https://firebase.google.com/docs/reference/js/firebase.firestore.Settings
  :get-user-sub      [:user]
  :set-user-event    [:set-user])