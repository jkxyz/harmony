(ns harmony.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(def sequence-length 16)

(def bars 4)

(defn empty-sequence []
  (into [] (repeat sequence-length 0)))

(defonce conn (js/WebSocket. "ws://localhost:3449/ws"))

;; EFFECTS

(rf/reg-fx
 ::send
 (fn [event]
   (.send conn (str event))))

;; EVENTS

(rf/reg-event-db
 ::init
 (fn [db _]
   {:sequences {:kick (empty-sequence)}}))

(rf/reg-event-fx
 ::button-on
 (fn [{:keys [db]} [_ voice beat-index]]
   {:db (assoc-in db [:sequences voice beat-index] 1)
    ::send [:button-on voice beat-index]}))

(rf/reg-event-fx
 ::button-off
 (fn [{:keys [db]} [_ voice beat-index]]
   {:db (assoc-in db [:sequences voice beat-index] 0)
    ::send [:button-off voice beat-index]}))

;; SUBSCRIPTIONS

(rf/reg-sub
 ::sequence
 (fn [db [_ voice]]
   (-> db :sequences voice)))

(defn button [{:keys [bar on? on-click]}]
  [:div
   {:on-click on-click
    :class ["keybutton"
            (if (even? bar) "keybutton-pink" "keybutton-purple")
            (when on? "keybutton-pressed")]}
   [:div.button-light]])

(defn tempo [bpm]
  [:div.tempo-display bpm])

(defn app []
  [:div.main
   [:h1 "h a r m o n y"]
   [:div.tempocontainer [tempo 50]] ; dummy value
   [:div.keyscontainer
    (for [[kick-val beat] (map list @(rf/subscribe [::sequence :kick]) (range 0 16))]
      [button {:key beat
               :bar (inc (Math/floor (/ beat bars)))
               :on? (= kick-val 1)
               :on-click #(rf/dispatch [(if (= kick-val 1) ::button-off ::button-on) :kick beat])}])]])

(defn mount-root []
  (r/render [app] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [::init])
  (mount-root))



















(comment[:slidercontainer [:input {:type "range"}
                           :min 1
                           :max 100
                           :value 50
                           :class "slider"
                           :id "frequency"]])
(comment "you need another div here")
(comment[:slidercontainer [:input {:type "range"}
                           :min 1
                           :max 100
                           :value 50
                           :class "slider"
                           :id "amplitude"]])
