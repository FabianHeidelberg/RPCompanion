(ns rp-companion.viewer
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [rp-companion.interceptors :refer [app-event-handler]]))

;; Event handlers
(rf/reg-event-db
  :viewer/initialize
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:room-id room-id}))


;; Subscriptions

(rf/reg-sub
  :viewer/entities
  (fn [_ _]
    (rf/subscribe [:firestore/on-snapshot {:path-document [:rooms "myquest"]}]))
  (fn [value _]
    (let [entities (vals (get-in value [:data "entities"])) ]
      (map
        (fn [entity] {:id (get entity "id")
                      :position (get entity "position")
                      :icon (get entity "icon")})
        entities))))

;; Views

(defn entity-view
  [{icon :icon
   [x y] :position
   id :id}]
  (println icon x y id)
  [:image.animated-entity {:transform (str "translate(" (- x 20) "," (- y 20) ")")
                            :width 40
                            :height 40
                            :href icon
                            :key id}])

(defn main-view []
  (let [entities @(rf/subscribe [:viewer/entities])]
    [:div.game-wrapper
      [:svg.game
        {:width 1366 :height 768 }
          (map entity-view entities)]]))
