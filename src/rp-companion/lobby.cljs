(ns rp-companion.lobby
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as string]
            [rp-companion.interceptors :refer [app-event-handler]]))

;; Effects

(rf/reg-fx
  :navigate-to
  (fn [path]
    (set! (.-hash js/window.location) path)))

;; Event handlers

(rf/reg-event-db
  :lobby/initialize
  [app-event-handler]
  (fn [_ _]
    {:room-id "" }))


(rf/reg-event-db
  :update-room-id
  [app-event-handler]
  (fn [db [_ value]]
    (assoc db :room-id value)))


(rf/reg-event-fx
  :join-game
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:navigate-to (str "viewer/" room-id) }))


(rf/reg-event-fx
  :create-game
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:navigate-to (str "master/" room-id)}))


;; Subscriptions

(rf/reg-sub :room-id (fn [db _] (get-in [:app :room-id] db)))

;; Views

(defn main-view []
  (let [ room-id @(rf/subscribe [:room-id])]
    [:div {}
      [:label {:for "room"} "Room: "]
      [:input {:id "room"
               :type "text"
               :value room-id
               :on-change (fn [evt]
                              (rf/dispatch [:update-room-id (-> evt .-target .-value)]))}]
      [:p
        [:button {:on-click #(rf/dispatch [:join-game room-id])
                  :disabled (string/blank? room-id)}
                  "join game"]
        [:button {:on-click #(rf/dispatch [:create-game room-id])
                  :disabled (string/blank? room-id)}
                  "create game"]]]))
