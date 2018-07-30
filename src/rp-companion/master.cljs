(ns rp-companion.master
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]))


;; Event handler

(rf/reg-event-db
  :initialize
  (fn [_ [_ room-id]]
    {:room-id room-id
     :entities {
                1 {:position [100 100] :color "red" :id 1}
                2 {:position [100 500] :color "blue" :id 2}
                3 {:position [50 20] :color "orang" :id 3}}
     :actions [{:type "move" :data {:position [30 120]} :creator "player" :entity-id 1}
               {:type "move" :data {:position [400 300]} :creator "player" :entity-id 3}]}))

(rf/reg-event-db
  :add-entity
  (fn [db _]
    (let [id (Math/random)
          x (* 300 (Math/random))
          y (* 300 (Math/random))]
    (assoc-in db [:entities id] {:position [x y] :color "yellow" :id id} ))))

;; Subscriptions

(rf/reg-sub
  :entities
  (fn [db _]
    (vals (:entities db))))

(rf/reg-sub
  :room-id
  (fn [db _]
    (:room-id db)))

(rf/reg-sub
  :actions
  (fn [db _]
    (map (fn [action]
            (let [entity-id (:entity-id action)]
              (assoc action :entity (get-in db [:entities])))))))


;; Views

(defn entity-view
  [{color :color
    [x y] :position
    id :id}]
  [:circle {:cx x :cy y :r 20 :fill color :key id}])

(defn entities-view [{:keys [entities]}]
  [:g {} (map entity-view entities)])

(defn menu-item-view [{icon :icon name :name}])
(def menu-items [{:label "enemies"
                  :type-instances [
                    {:icon "orc.svg" :name "Orc"}
                    {:icon "goblin.svg" :name "Goblin"}
                    {:icon "spider.svg" :name "Spider"}
                    {:icon "wolf.svg" :name "Wolf"}]}
                {:label "objects"
                :type-instances [
                  {:icon "treasure-chest.svg" :name "Treasure chest"}
                  {:icon "campfire.svg" :name "Campfire"}
                  {:icon "bag.svg" :name "Bag"}]}
                {:label "players"
                :type-instances [
                  {:icon "sourcerer.svg" :name "sourcerer"}
                  {:icon "knight.svg" :name "Knight"}
                  {:icon "paladine.svg" :name "Paladine"}
                  {:icon "Barbar.svg" :name "Barbar"}]}])

(defn main-view []
  (let [entities @(rf/subscribe [:entities])
        room-id @(rf/subscribe [:room-id])]
    [:div
      [:h1 room-id]
      [:svg
        {:width 500 :height 500}
        [entities-view {:entities entities}]]
        [:button
          {:on-click #(rf/dispatch [:add-entity])} "Add Entity"]]))

