(ns rp-companion.lobby
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as string]
            [rp-companion.interceptors :refer [app-event-handler]]))

;; Effects

(rf/reg-fx
  :lobby/navigate-to
  (fn [path]
    (set! (.-hash js/window.location) path)))

;; Event handlers

(rf/reg-event-db
  :lobby/initialize
  [app-event-handler]
  (fn [_ _]
    {:room-id "" }))

(rf/reg-event-db
  :lobby/update-room-id
  [app-event-handler]
  (fn [db [_ value]]
    (assoc db :room-id value)))

(rf/reg-event-fx
  :lobby/join-game
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:lobby/navigate-to (str "viewer/" room-id) }))

(rf/reg-event-fx
  :lobby/create-game
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:lobby/navigate-to (str "master/" room-id)}))


;; Subscriptions

(rf/reg-sub :lobby/room-id (fn [db _]
  (get-in db [:app :room-id])))

;; Views

(defn main-view []
  (let [ room-id @(rf/subscribe [:lobby/room-id])]
    [:div.lobby.pure-form.pure-form-stacked {}
      [:div.lobby-header
        [:img {:src "./assets/barbarian.svg"}]
        [:h1 "RP Companion"]
        [:img {:src "./assets/chest.svg"}]]
      [:label {:for "room"} "Room"]
      [:input {:id "room"
               :type "text"
               :value room-id
               :on-change (fn [evt]
                              (rf/dispatch [:lobby/update-room-id (-> evt .-target .-value)]))}]
      [:p
        [:button.pure-button {:on-click #(rf/dispatch [:lobby/join-game room-id])
                  :disabled (string/blank? room-id)}
                  "join game"]
        [:span " "]
        [:button.pure-button {:on-click #(rf/dispatch [:lobby/create-game room-id])
                  :disabled (string/blank? room-id)}
                  "create game"]]]))
