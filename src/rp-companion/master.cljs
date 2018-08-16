(ns rp-companion.master
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [rp-companion.utils :as utils]))


;; Event handler

(rf/reg-event-db
  :master/initialize
  (fn [_ [_ room-id]]
    {:room-id room-id
     :entities {
                1 {:position [100 100] :color "red" :id 1 :actions {:next-position nil :will-be-deleted false}}
                2 {:position [100 500] :color "blue" :id 2 :actions {:next-position nil :will-be-deleted false}}
                3 {:position [50 20] :color "orang" :id 3 :actions {:next-position nil :will-be-deleted false}}}
      :grabbed-entity nil
      :selected-entity nil}))

(rf/reg-event-db
  :add-entity
  (fn [db _]
    (let [id (Math/random)
          x (* 300 (Math/random))
          y (* 300 (Math/random))]
    (assoc-in db [:entities id] {:position [x y] :color "yellow" :id id} ))))

(rf/reg-event-db
  :delete-entity
 (fn [db [_ id]]
    (do
      (rf/dispatch [:release-entity])
      (rf/dispatch [:deselect-entity])
      (utils/dissoc-in db [:entities id]))))

(rf/reg-event-db
  :grab-entity
 (fn [db [_ id]]
       (assoc db :grabbed-entity {:id id :moved false :timestamp (.getTime (js/Date.))})))

(rf/reg-event-db
  :select-entity
  (fn [db [_ id]]
      (assoc db :selected-entity {:id id})))

(defn apply-position [db id]
      (let [entity (get-in db [:entities id])]
        (if-not (nil? (get-in entity [:actions :next-position])) 
          (assoc-in db [:entities id :position] (get-in entity [:actions :next-position])) 
          db)))

(rf/reg-event-db
  :release-entity
  (fn [db] 
  (let [timestamp (get-in db [:grabbed-entity :timestamp]) 
        time-diff (- (.getTime (js/Date.)) timestamp)] 
    (if (or (get-in db [:grabbed-entity :move]) (> time-diff 200)) 
      (-> (assoc db :grabbed-entity nil))
      (-> db 
        (apply-position (get-in db [:grabbed-entity :id]))
        (assoc :grabbed-entity nil))))))

(rf/reg-event-db
  :deselect-entity
  (fn [db]
    (assoc db :selected-entity nil)))


 (rf/reg-event-db
   :update-next-position
   (fn [db [_ data]]
       (let [id (:id data)
             position (:position data)]
            (-> db 
              (assoc-in [:grabbed-entity :moved] true)
              (assoc-in [:entities id :actions :next-position] position)))))

(rf/reg-event-db
  :apply-next-pos
  (fn [db _]
      (let [entities (:entities db)
            updated-entities (->> (map (fn [[key entity]]
                                        (let [old-position (:position entity)
                                               new-position (get-in entity [:actions :next-position])]
                                          [key (-> entity
                                                (assoc :position (or new-position old-position))
                                                (assoc-in [:actions :next-position] nil))])) entities)
                                 (flatten)
                                 (apply hash-map))]
             (do
              (rf/dispatch [:deselect-entity])
              (rf/dispatch [:release-entity])
              (assoc db :entities updated-entities)))))


(rf/reg-event-db
  :fast-apply-next-pos
  (fn [db [_ id]]
      (let [entity (get-in db [:entities id])]
        (if-not (nil? (get-in entity [:actions :next-position]))
          (assoc-in db [:entities id :position] (get-in entity [:actions :next-position]))
          db))))

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
  :grabbed-entity
  (fn [db _]
    (:grabbed-entity db)))

(rf/reg-sub
  :selected-entity
  (fn [db _]
    (:selected-entity db)))
;; Views

(defn entity-view
  [ {grabbed-entity-id :grabbed-entity-id
    selected-entity-id :selected-entity-id}
    {color :color
    [x y] :position
    actions :actions
    id :id}]
      (let [next-pos (:next-position actions)
            [next-x next-y] next-pos]
           [:g {:key id}
            (when (some? next-pos)
              [:line.connector {:x1 x
                                :y1 y
                                :x2 next-x
                                :y2 next-y}])
            (when (some? next-pos)
              [:circle.ghost-entity {:transform (str "translate(" next-x "," next-y ")")
                                     :r 20
                                     :fill color
              :on-mouse-down #(rf/dispatch [:grab-entity id])
              :on-touch-start #(rf/dispatch [:grab-entity id])}])
            [:circle.animated-entity {:transform (str "translate(" x "," y ")")
                                      :r 20
                                      :fill color
                                      :on-mouse-down (fn []
                                        (rf/dispatch [:select-entity id])
                                        (rf/dispatch [:grab-entity id]))
                                      :on-touch-start (fn []
                                        (rf/dispatch [:select-entity id])
                                        (rf/dispatch [:grab-entity id]))}]
            (when (= selected-entity-id id)
              [:circle.animated-entity.delete-symbol {:transform (str "translate(" (+ x 15) ", " (- y 20) ")")
                                      :r 10
                                      :fill "#fffff"
                                      :on-mouse-down #(rf/dispatch [:delete-entity id])
                                      :on-touch-start #(rf/dispatch [:delete-entity id])}])]))

(defn entities-view [{:keys [entities grabbed-entity-id selected-entity-id]}]
  [:g {} (map (partial entity-view {:grabbed-entity-id grabbed-entity-id
                                    :selected-entity-id selected-entity-id}) entities)])

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
        room-id @(rf/subscribe [:room-id])
        grabbed-entity @(rf/subscribe [:grabbed-entity])
        grabbed-entity-id (:id grabbed-entity)
        selected-entity @(rf/subscribe [:selected-entity])
        selected-entity-id (:id selected-entity)]
       [:div
        [:svg
         {:width 500 :height 500 
          :on-mouse-move (fn [event] (let  [x (.-clientX event)
                                            y (.-clientY event)]
                                          
                                          (if-not (nil? grabbed-entity-id)
                                        (rf/dispatch [:update-next-position {:id grabbed-entity-id :position [x y]}]))))
        :on-touch-move (fn [event]
                          (let [touches (.. event -changedTouches)
                                touch (.item touches 0)
                                touch-x (.-clientX touch)
                                touch-y (.-clientY touch)]
                            (if-not (nil? grabbed-entity-id)  
                            (rf/dispatch [:update-next-position {:id grabbed-entity-id :position [touch-x touch-y]}]))))
        :on-mouse-up #(rf/dispatch [:release-entity])
        :on-touch-end #(rf/dispatch [:release-entity])
        :on-click (fn [] (when (nil? grabbed-entity)
                          (rf/dispatch [:deselect-entity])))}
         [entities-view {:entities entities
                        :grabbed-entity-id grabbed-entity-id
                        :selected-entity-id selected-entity-id}]]
        [:button
         {:on-click #(rf/dispatch [:add-entity])} "Add Entity"]
        [:button
         {:on-click #(rf/dispatch [:apply-next-pos])} "Apply changes"]]))

