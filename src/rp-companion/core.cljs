(ns rp-companion.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]))

;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:entities {}
        1 {:position [100 100] :color "red" :id 1}
        2 {:position [100 500] :color "blue" :id 2}
        3 {:position [50 20] :color "orang" :id 3}
     :actions [{:type "move" :data {:position [30 120]} :creator "player" :entity-id 1}
               {:type "move" :data {:position [400 300]} :creator "player" :entity-id 3}]}))


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :entities
  (fn [db _]     ;; db is current app state. 2nd unused param is query vector
    (vals (:entities db)))) ;; return a query computation over the application state

(rf/reg-sub :actions
            fn [db _]
            ((map (fn [action]
                    (let [entity-id (:entity-id action)]
                      (assoc action :entity (get-in db [:entities])))))))


;; -- Domino 5 - View Functions ----------------------------------------------


(defn entityView [{color :color
                   [x y] :position
                    id :id}]
  [:circle {:cx x
            :cy y
            :r 20
            :fill color
            :key id}])

(defn entitiesView [{:keys [entities]}]
  (let [entities @(rf/subscribe [:entities])]
    [:g {} (map entityView entities)]))

(defn ui []
  [:svg
      {:width 500 :height 500}
      [entitiesView]])


;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (reagent/render [ui]              ;; mount the application's ui into '<div id="app" />'
                  (js/document.getElementById "app")))
