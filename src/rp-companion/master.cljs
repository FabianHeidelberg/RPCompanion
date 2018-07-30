(ns rp-companion.master
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]))


;; Event handler

(rf/reg-event-db
  :initialize
  (fn [_ [_ room-id]]
    {:room-id room-id
     :entities {
                1 {:position [100 100] :color "red" :id 1 :actions {:next-position [100 100] :will-be-deleted false}}
                2 {:position [100 500] :color "blue" :id 2 :actions {:next-position [100 500] :will-be-deleted false}}
                3 {:position [50 20] :color "orang" :id 3 :actions {:next-position [50 20] :will-be-deleted false}}}}))

(rf/reg-event-db
  :add-entity
  (fn [db _]
    (let [id (Math/random)
          x (* 300 (Math/random))
          y (* 300 (Math/random))]
    (assoc-in db [:entities id] {:position [x y] :color "yellow" :id id} ))))

 (rf/reg-event-db
   :update-next-position
   (fn [db [_ data]]
       (let [id (:id data)
             position (:position data)]
            (do
              (println "updated entity" id position db)
              (assoc-in db [:entities id :actions :next-position] position)))))

;; Subscriptions

(rf/reg-sub
  :entities
  (fn [db _]
    (vals (:entities db))))

(rf/reg-sub
  :room-id
  (fn [db _]
    (:room-id db)))

;; Views

(defn entity-view
  [{color :color
    [x y] :position
    actions :actions
    id :id}]
      (let [next-pos (:next-position actions)
            next-x (first next-pos)
            next-y (second next-pos)]
           [:g {:key id}
            [:circle {:cx x
                      :cy y
                      :r 20
                      :fill color
                      :on-touch-start (fn [event]
                                          (let [touches (.. event -changedTouches)
                                                touch (.item touches 0)
                                                touch-x (.-clientX touch)
                                                touch-y (.-clientY touch)]
                                               (print "clicked " id)))
                      :on-touch-move (fn [event]
                                         (let [touches (.. event -changedTouches)
                                               touch (.item touches 0)
                                               touch-x (.-clientX touch)
                                               touch-y (.-clientY touch)]
                                              (do
                                                (println "moved" id touch-x touch-y)
                                                (rf/dispatch [:update-next-position {:id id :position [touch-x touch-y]}]))))
                      :on-touch-end (fn [event]
                                        (let [touches (.. event -changedTouches)
                                              touch (.item touches 0)
                                              touch-x (.-clientX touch)
                                              touch-y (.-clientY touch)]
                                             (println "ended" id)))}]
           [:circle {:cx next-x
                     :cy next-y
                     :r 10
                     :fill "#000"
                     }]]))

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
      [:svg
        {:width 500 :height 500}
        [entities-view {:entities entities}]]
        [:button
          {:on-click #(rf/dispatch [:add-entity])} "Add Entity"]]))

