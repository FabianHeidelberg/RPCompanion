(ns rp-companion.master
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [rp-companion.utils :as utils]
            [rp-companion.interceptors :refer [app-event-handler]]))

;; Event handler

(rf/reg-event-db
  :master/initialize
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:room-id room-id
     :entities {}
      ;; Example for an entity:
      ;; { 1 {:position [200 200] :icon "./assets/orc.svg" :id 1 :actions {:next-position [100 100]}} }
      ;;
      :menu nil
      :selection-state nil
      ;;
      ;; 3 possible states
      ;;
      ;; Examples:
      ;; {:state :none}
      ;; {:state :grabbed :id 1 :start-position [x y] :moved true :timestamp 123123 :is-ghost false}
      ;; {:state :selected :id 1 :is-ghost false}")
      ;;
      :grabbed-entity nil
      :selected-entity nil}))

(rf/reg-event-fx
  :master/publish-state
  (fn [{{{:keys [room-id entities]} :app} :db}]
    {:firestore/set {:path [:rooms room-id]
                     :data {:entities entities}
                     :set-options {:merge false}
                     :on-success #(js/console.log "published new state")
                     :on-failure #(js/console.log "failed to publish state")}}))

(rf/reg-event-fx
  :master/add-entity
  [app-event-handler]
  (fn [{db :db} [_ {:keys [text icon position]}]]
    (let [id (rand)]
      {:db (-> db
        (assoc-in [:entities id] {:position position :icon icon :id id})
             (dissoc :menu))
      :dispatch [:master/publish-state]})))

(rf/reg-event-fx
  :master/delete-entity
  [app-event-handler]
  (fn [{db :db} [_ id]]
    {:db (utils/dissoc-in db [:entities id])
     :dispatch [:master/publish-state]}))

(rf/reg-event-db
  :master/delete-ghost
  [app-event-handler]
  (fn [db [_ id]]
      (do
        (utils/dissoc-in db [:entities id :actions :next-position]))))

(rf/reg-event-db
  :master/grab-entity
  [app-event-handler]
  (fn [db [_ {:keys [id position]}]]
      (assoc db :selection-state {:state :grabbed
                                  :id id
                                  :moved false
                                  :start-position position
                                  :timestamp (.getTime (js/Date.))
                                  :is-ghost false})))

(rf/reg-event-db
  :master/grab-ghost
  [app-event-handler]
  (fn [db [_ {:keys [id position]}]]
    (assoc db :selection-state {:state :grabbed
                                :id id
                                :moved false
                                :start-position position
                                :timestamp (.getTime (js/Date.))
                                :is-ghost true})))

(rf/reg-event-db
  :master/toggle-menu
  [app-event-handler]
  (fn [db [_ [x y]]]
    (println "toggle menu")
    (if (nil? (:menu db)) (assoc db :menu {:position [x y]})
      (dissoc db :menu))))

(rf/reg-event-db
  :master/select-menu-type
  [app-event-handler]
  (fn [db [_ type]]
    (assoc-in db [:menu :type] type)))


(defn apply-position [db id]
      (let [entity (get-in db [:entities id])]
        (if-not (nil? (get-in entity [:actions :next-position]))
          (assoc-in db [:entities id :position] (get-in entity [:actions :next-position]))
          db)))

(rf/reg-event-fx
  :master/release-entity
  [app-event-handler]
  (fn [{db :db}]
    {:db (let [selection-state (:selection-state db)]
      (if (= (:state selection-state) :grabbed)
        (let [id (:id selection-state)
              timestamp (:timestamp selection-state)
              time-diff (- (.getTime (js/Date.)) timestamp)
              is-ghost (:is-ghost selection-state)
              moved (:moved selection-state)]
          ;; move => change state to unselected
          (if moved
            (assoc db :selection-state {:state :none})
            ;; ... clicks and long clicks handle differently for ghost and entity
            (if (:is-ghost selection-state)
              (if (>= time-diff 500)
                ;; long click for delete ghost option
                (assoc db :selection-state {:state :selected :is-ghost true :id id})
                ;; quick apply next position,
                (-> db
                  (apply-position id)
                  (assoc :selection-state {:state :none})))
              (if (>= time-diff 500)
                ;;long click for delete entity option
                (assoc db :selection-state {:state :selected :is-ghost false :id id})
                ;;short click release entity
                (assoc db :selection-state {:state :none})))))
              db))
     :dispatch [:master/publish-state]}))


(rf/reg-event-db
  :master/update-next-position
  [app-event-handler]
  (fn [db [_ data]]
    (let [id (:id data)
            [x y] (:position data)
            selection-state (:selection-state db)]
          (if (= (:state selection-state) :grabbed)
            (let [[start-x start-y] (:start-position selection-state)
                  moved (:moved selection-state)
                  distance (Math/sqrt (+ (Math.pow (- x start-x) 2) (Math.pow (- y start-y) 2)))]
              (if (or moved (> distance 5))
                (-> db
                  (assoc-in [:selection-state :moved] true)
                  (assoc-in [:entities id :actions :next-position] [x y]))
                db))
                db))))

(rf/reg-event-fx
  :master/apply-next-pos
  [app-event-handler]
  (fn [{db :db} _]
      {:db (let [entities (:entities db)
                 updated-entities (->> (map (fn [[key entity]]
                                        (let [old-position (:position entity)
                                               new-position (get-in entity [:actions :next-position])]
                                          [key (-> entity
                                                (assoc :position (or new-position old-position))
                                                (assoc-in [:actions :next-position] nil))])) entities)
                                 (flatten)
                                 (apply hash-map))]
              (assoc db :entities updated-entities))
      :dispatch [:master/publish-state]}))


;; Subscriptions

(rf/reg-sub
  :master/app
  (fn [db _]
    (:app db)))

(rf/reg-sub
  :master/entities
  #(rf/subscribe [:master/app])
  (fn [app]
    (vals (:entities app))))

(rf/reg-sub
  :master/room-id
  #(rf/subscribe [:master/app])
  (fn [app _]
    (:room-id app)))

(rf/reg-sub
  :master/grabbed-entity-id
  #(rf/subscribe [:master/app])
  (fn [app]
    (let [selection-state (:selection-state app)]
      (if (= (:state selection-state) :grabbed)
        (:id selection-state)))))

(rf/reg-sub
  :master/selected-entity
  #(rf/subscribe [:master/app])
  (fn [app]
    (let [selection-state (:selection-state app)]
      (if (= (:state selection-state) :selected)
        {:id (:id selection-state)
         :is-ghost (:is-ghost selection-state)}))))

(rf/reg-sub
  :master/menu
  #(rf/subscribe [:master/app])
  (fn [app _]
    (:menu app)))


;; Views

(defn entity-view
  [ {:keys [grabbed-entity-id selected-entity]}
    {icon :icon
     [x y] :position
     actions :actions
     id :id}]
  (let [next-pos (:next-position actions)
        [next-x next-y] next-pos
        selected-entity-id (:id selected-entity)
        is-ghost (:is-ghost selected-entity)]
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
                                 :on-mouse-down (fn [event]
                                                  (let  [x (.-clientX event)
                                                         y (.-clientY event)]
                                                          (rf/dispatch [:master/grab-ghost {:id id :position [x y]}])))
                                 :on-touch-start (fn [event]
                                                    (let [touches (.. event -changedTouches)
                                                          touch (.item touches 0)
                                                          touch-x (.-clientX touch)
                                                          touch-y (.-clientY touch)]
                                                            (rf/dispatch [:master/grab-ghost {:id id :position [x y]}])))
                                 :on-click #(.stopPropagation %)
                                 :on-drag-start #(constantly false)
                                 :on-drag-end #(constantly false)}])
        [:image.animated-entity {:transform (str "translate(" (- x 20) "," (- y 20) ")")
                                  :width 40
                                  :height 40
                                  :href icon
                                  :on-drag-start #(constantly false)
                                  :on-drag-end #(constantly false)
                                  :on-mouse-down (fn [event]
                                                  (let  [x (.-clientX event)
                                                        y (.-clientY event)]
                                                          (rf/dispatch [:master/grab-entity {:id id :position [x y]}])))
                                  :on-touch-start (fn [event]
                                                    (let [touches (.. event -changedTouches)
                                                          touch (.item touches 0)
                                                          touch-x (.-clientX touch)
                                                          touch-y (.-clientY touch)]
                                                            (rf/dispatch [:master/grab-entity {:id id :position [x y]}])))}]
        (when (and (= selected-entity-id id) is-ghost )
              [:image {:transform (str "translate(" (+ next-x 7) ", " (- next-y 32) ")")
                       :width 32
                       :height 32
                       :href "./assets/delete-icon.svg"
                       :on-click (fn [evt]
                                    (.stopPropagation evt)
                                    (rf/dispatch [:master/delete-ghost id]))}])
        (when (and (= selected-entity-id id) (not is-ghost) )
              [:image {:transform (str "translate(" (+ x 7) ", " (- y 32) ")")
                       :width 32
                       :height 32
                       :href "./assets/delete-icon.svg"
                       :on-click (fn [evt]
                                    (.stopPropagation evt)
                                    (rf/dispatch [:master/delete-entity id]))}])]))


(defn entities-view [{:keys [entities grabbed-entity-id selected-entity]}]

  [:g {} (map (partial entity-view {:grabbed-entity-id grabbed-entity-id
                                    :selected-entity selected-entity}) entities)])

(defn menu-item-view [{icon :icon name :name}])
(def menu-items {:enemies {:label "enemies"
                           :type-instances [
                                            {:icon "./assets/orc.svg" :name "Orc"}
                                            {:icon "./assets/spider.svg" :name "Spider"}
                                            {:icon "./assets/wolf.svg" :name "Wolf"}]}
                 :objects {:label "objects"
                           :type-instances [
                                            {:icon "./assets/chest.svg" :name "Treasure chest"}
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
                               (rf/dispatch [:master/select-menu-type :enemies]))}]
    [:image {:transform (translate-helper {:deg 120 :r 50})
              :href "./assets/sack.svg"
              :x -20
              :y -20
              :width 40
              :height 40
              :on-click (fn [evt]
                            (.stopPropagation evt)
                            (rf/dispatch [:master/select-menu-type :objects]))}]
    [:image {:transform (translate-helper {:deg 240 :r 50})
              :href "./assets/sword.svg"
              :x -20
              :y -20
              :width 40
              :height 40
              :on-click (fn [evt]
                            (.stopPropagation evt)
                            (rf/dispatch [:master/select-menu-type :players]))}]
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
                            (rf/dispatch [:master/select-menu-type :objects]))}]]

   [:g [:g (map-indexed (fn [index item] [:image {:key index
                                                  :transform (translate-helper {:deg (* index (/ 360 (count (get-in menu-items [type :type-instances])))) :r 50})
                                                  :href (:icon item)
                                                  :width 40
                                                  :height 40
                                                  :x -20
                                                  :y -20
                                                  :on-click #(rf/dispatch [:master/add-entity {:position [x y] :icon (:icon item)}])}]) (get-in menu-items [type :type-instances]))]
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
                               (rf/dispatch [:master/select-menu-type :objects]))}]])])

(defn main-view []
  (let [entities @(rf/subscribe [:master/entities])
        room-id @(rf/subscribe [:master/room-id])
        grabbed-entity-id @(rf/subscribe [:master/grabbed-entity-id])
        selected-entity @(rf/subscribe [:master/selected-entity])
        menu @(rf/subscribe [:master/menu])]
       [:div.game-wrapper
        [:svg.game
         {:width 1366 :height 768 :id "background"
          :on-mouse-move (fn [event] (let  [x (.-clientX event)
                                            y (.-clientY event)]
                                          (if-not (nil? grabbed-entity-id)
                                           (rf/dispatch [:master/update-next-position {:id grabbed-entity-id :position [x y]}]))))
          :on-touch-move (fn [event]
                            (let [touches (.. event -changedTouches)
                                  touch (.item touches 0)
                                  touch-x (.-clientX touch)
                                  touch-y (.-clientY touch)]
                              (if-not (nil? grabbed-entity-id)
                               (rf/dispatch [:master/update-next-position {:id grabbed-entity-id :position [touch-x touch-y]}]))))
          :on-click (fn [event] (let [x (.-clientX event)
                                      y (.-clientY event)
                                      id (.. event -target -id)]
                                     (if (= id "background") (rf/dispatch [:master/toggle-menu [x y]]))))
          :on-mouse-up #(rf/dispatch [:master/release-entity])
          :on-mouse-leave (fn [event]
                          (let [id (.. event -target -id)]
                            (if (= id "background") (rf/dispatch [:master/release-entity]))))
          :on-touch-end #(rf/dispatch [:master/release-entity])}
         [entities-view {:entities entities :grabbed-entity-id grabbed-entity-id :selected-entity selected-entity}]
         (if-not (nil? menu)[menu-view menu])]
        [:button.move-all-button.pure-button
         {:on-click #(rf/dispatch [:master/apply-next-pos])} "move all"]]))
