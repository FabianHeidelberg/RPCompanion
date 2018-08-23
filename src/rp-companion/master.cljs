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
  (fn [db [_ {:keys [text icon position]}]]
    (let [id (rand)]
      (-> db
        (assoc-in [:entities id] {:position position :icon icon :id id})
        (dissoc :menu)))))

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

(rf/reg-event-db 
  :toggle-menu 
  (fn [db [_ [x y]]]
   (if (nil? (:menu db)) (assoc db :menu {:position [x y]}) 
     (dissoc db :menu))))


(rf/reg-event-db
  :select-menu-type
  (fn [db [_ type]] 
    (assoc-in db [:menu :type] type)))


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
     (if (or (get-in db [:grabbed-entity :moved]) (> time-diff 200)) 
       (assoc db :grabbed-entity nil) 
       (-> db 
         (apply-position (get-in db [:grabbed-entity :id]))
         (assoc :grabbed-entity nil))))))

(rf/reg-event-db
  :deselect-entity
  (fn [db]
    (assoc db :selected-entity nil)))

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
  :menu 
  (fn [db _]
    (:menu db)))

(rf/reg-sub
  :selected-entity
  (fn [db _]
    (:selected-entity db)))
;; Views

(defn entity-view
  [ grabbed-entity
    {icon :icon
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
          [:image.ghost-entity {:transform (str "translate(" (- next-x 20 ) "," (- next-y 20) ")")
                                 :width 40
                                 :height 40
                                 :href icon
                                 :on-mouse-down #(rf/dispatch [:grab-entity id])
                                 :on-touch-start #(rf/dispatch [:grab-entity id])
                                 :on-click #(.stopPropagation %)
                                 :on-drag-start #(constantly false)
                                 :on-drag-end #(constantly false)}])
        [:image.animated-entity {:transform (str "translate(" (- x 20) "," (- y 20) ")")
                                  :width 40
                                  :height 40
                                  :href icon
                                  :on-mouse-down #(rf/dispatch [:grab-entity id])
                                  :on-touch-start #(rf/dispatch [:grab-entity id])}]]
        (when (= selected-entity-id id)
              [:circle.animated-entity.delete-symbol {:transform (str "translate(" (+ x 15) ", " (- y 20) ")")}
                                      :r 10
                                      :fill "#fffff"
                                      :on-mouse-down #(rf/dispatch [:delete-entity id])
                                      :on-touch-start #(rf/dispatch [:delete-entity id])}])))

(defn entities-view [{:keys [entities grabbed-entity-id selected-entity-id]}]
  [:g {} (map (partial entity-view {:grabbed-entity-id grabbed-entity-id
                                    :selected-entity-id selected-entity-id}) entities)])

(defn menu-item-view [{icon :icon name :name}])
(def menu-items {:enemies {:label "enemies"
                           :type-instances [
                                            {:icon "./assets/orc.svg" :name "Orc"}
                                            {:icon "./assets/goblin.svg" :name "Goblin"}
                                            {:icon "./assets/spider.svg" :name "Spider"}
                                            {:icon "./assets/wolf.svg" :name "Wolf"}]}
                 :objects {:label "objects"
                           :type-instances [
                                            {:icon "./assets/treasure-chest.svg" :name "Treasure chest"}
                                            {:icon "./assets/campfire.svg" :name "Campfire"}
                                            {:icon "./assets/sack.svg" :name "Bag"}]}
                 :players {:label "players"
                           :type-instances [
                                            {:icon "./assets/wizard.svg" :name "Wizard"}
                                            {:icon "./assets/knight.svg" :name "Knight"}
                                            {:icon "./assets/thief.svg" :name "Thief"}
                                            {:icon "./assets/barbarian.svg" :name "Barbar"}]}})

(defn deg-to-rad [deg] (* Math/PI (/ deg 180))) 
  

(defn translate-helper [{:keys [deg r]}] 
 (let [x (* r (Math/cos (deg-to-rad deg)))
       y (* r (Math/sin (deg-to-rad deg)))] 
      (str "translate ("x "," y")")))
  
(defn menu-view [{[x y] :position type :type}] 
 [:g {:transform (str "translate("x "," y")")} 
  (if (nil? type) 
   [:g [:image {:transform (translate-helper {:deg 0 :r 50})
                 :href "./assets/orc.svg"
                 :x -20
                 :y -20 
                 :width 40 
                 :height 40        
                 :on-click (fn [evt] 
                               (.stopPropagation evt)
                               (rf/dispatch [:select-menu-type :enemies]))}]
    [:image {:transform (translate-helper {:deg 120 :r 50})
              :href "./assets/sack.svg"
              :x -20
              :y -20
              :width 40
              :height 40
              :on-click (fn [evt] 
                            (.stopPropagation evt)
                            (rf/dispatch [:select-menu-type :objects]))}]
    [:image {:transform (translate-helper {:deg 240 :r 50})
              :href "./assets/sword.svg"
              :x -20
              :y -20
              :width 40
              :height 40
              :on-click (fn [evt] 
                            (.stopPropagation evt)
                            (rf/dispatch [:select-menu-type :players]))}]
    [:circle {:transform (str "rotate(0) translate(0, 0)")
              :href (str "./assets/sack.svg")
              :cx 0
              :cy 0
              :r 20
              :stroke (str "black")
              :stroke-width 1
              :stroke-dasharray (str "5,5")
              :fill (str "none")
              :on-click (fn [evt] 
                            (.stopPropagation evt)
                            (rf/dispatch [:select-menu-type :objects]))}]] 
  
   [:g [:g (map-indexed (fn [index item] [:image {:transform (translate-helper {:deg (* index (/ 360 (count (get-in menu-items [type :type-instances])))) :r 50})
                                               :href (:icon item)
                                               :width 40
                                               :height 40
                                               :x -20
                                               :y -20
                                               :on-click #(rf/dispatch [:add-entity {:position [x y] :icon (:icon item)}])}]) (get-in menu-items [type :type-instances]))]
        [:circle {:transform (str "rotate(0) translate(0, 0)")
              :href (str "./assets/sack.svg")
              :cx 0
              :cy 0
              :r 20
              :stroke (str "black")
              :stroke-width 1
              :stroke-dasharray (str "5,5")
              :fill (str "none")
              :on-click (fn [evt] 
                            (.stopPropagation evt)
                            (rf/dispatch [:select-menu-type :objects]))}]])])

(defn main-view []
  (let [entities @(rf/subscribe [:entities])
        room-id @(rf/subscribe [:room-id])
        grabbed-entity @(rf/subscribe [:grabbed-entity])
        selected-entity-id (:id selected-entity)
        menu @(rf/subscribe [:menu])]
        

       [:div
        [:svg
         {:width 500 :height 500 :id "background" 
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
          :on-click (fn [event] (let [x (.-clientX event)
                                      y (.-clientY event)
                                      id (.. event -target -id)]
                                     (if (= id "background") (rf/dispatch [:toggle-menu [x y]]))))
          :on-mouse-up #(rf/dispatch [:release-entity])}
         [entities-view {:entities entities :grabbed-entity-id grabbed-entity-id}]
         (if-not (nil? menu)[menu-view menu])]
        :on-touch-end #(rf/dispatch [:release-entity])
        [:button
         {:on-click #(rf/dispatch [:add-entity])} "Add Entity"]
        [:button
         {:on-click #(rf/dispatch [:apply-next-pos])} "Apply changes"]]))

