(ns rp-companion.interceptors
  (:require [re-frame.core :as rf]))

(def app-event-handler
  (rf/->interceptor
  :id :app-event-handler
  :before  (fn [context]
              (let [prev-db (get-in context [:coeffects :db])]
                (-> context
                  (assoc-in [:coeffects :db] (:app prev-db))
                  (assoc :prev-db prev-db))))
  :after (fn [context]
           (let [prev-db (:prev-db context)
                 app (get-in context [:effects :db])
                 new-db (assoc prev-db :app app)]
            (assoc-in context [:effects :db] new-db)))))

