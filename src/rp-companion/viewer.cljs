(ns rp-companion.viewer
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [rp-companion.interceptors :refer [app-event-handler]]))

;; Event handlers

(rf/reg-event-fx
  :example-2-listen
  (fn [_ _] {:firestore/on-snapshot {:path-document [:rooms "foobar"]
                                     :doc-changes true
                                     :on-next #(print "next")}}))


(rf/reg-event-db
  :viewer/initialize
  [app-event-handler]
  (fn [_ [_ room-id]]
    {:room-id room-id}))


;; Subscriptions

(rf/reg-sub
  :example-1-value2s
  (fn [_ _]
    (rf/subscribe [:firestore/on-snapshot {:path-document [:rooms "foobar"]}]))
  (fn [value _]
    value))

;; Views


(defn main-view []
  (let [value2 @(rf/subscribe [:example-1-value2])
  ]
    [:div {}
      [:h1 "viewer" ]
      [:p (str value2)]
      ]))
