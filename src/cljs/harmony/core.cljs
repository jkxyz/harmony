(ns harmony.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.edn :as edn]))

(def sequence-length 16)

(def bars 4)

(defn empty-sequence []
  (into [] (repeat sequence-length 0)))

(defonce conn (js/WebSocket. "ws://localhost:3449/ws"))

(.addEventListener
 conn
 "message"
 (fn [event]
   (println "Received:" (.-data event))
   (rf/dispatch (edn/read-string (.-data event)))))

;; EFFECTS

(rf/reg-fx
 :send
 (fn [event]
   (.send conn (str event))))

;; SERVER EVENTS

(rf/reg-event-db
 :server/init
 (fn [db [_ initial-db]]
   (assoc db :server initial-db)))

(rf/reg-event-db
 :server/button-on
 (fn [db [_ voice beat-index]]
   (assoc-in db [:server :sequences voice beat-index] 1)))

(rf/reg-event-db
 :server/button-off
 (fn [db [_ voice beat-index]]
   (assoc-in db [:server :sequences voice beat-index] 0)))

;; UI EVENTS

(rf/reg-event-db
  :init
  (fn [db _]
    (-> db
        (assoc :selected-voice :kick))))

(rf/reg-event-fx
 :button-on
 (fn [{:keys [db]} [_ voice beat-index]]
   {:send [:button-on voice beat-index]}))

(rf/reg-event-fx
 :button-off
 (fn [{:keys [db]} [_ voice beat-index]]
   {:send [:button-off voice beat-index]}))

(rf/reg-event-db
  :select-voice
  (fn [db [_ voice]]
    (assoc db :selected-voice voice)))


;; SUBSCRIPTIONS

(rf/reg-sub
 ::sequence
 (fn [db [_ voice]]
   (-> db :server :sequences voice)))

(rf/reg-sub
  ::selected-voice
  (fn [db _]
    (:selected-voice db)))

;; COMPONENTS

(defn sequencer-button [{:keys [bar on? on-click]}]
  [:div
   {:on-click on-click
    :class ["keybutton"
            (if (even? bar) "keybutton-pink" "keybutton-purple")
            (when on? "keybutton-pressed")]}
   [:div.button-light]])

(defn pattern-button [{:keys [voice selected?]} & children]
  [:div
   {:on-click #(rf/dispatch [:select-voice voice])
    :class ["instrument-button"
            (when selected? "instrument-button-pressed")]}
   children]) 

(defn tempo [bpm]
  [:div.tempocontainer [:div.tempo-display bpm]])

(defn app []
  (let [selected @(rf/subscribe [::selected-voice])
        selected-seq @(rf/subscribe[::sequence selected])]
    [:div.main
     [:h1 "h a r m o n y"]
     [tempo 50] ; dummy value
     [:div.slider-wrapper [:input {:type "range"}]
                       :min 1
                       :max 100
                       :value 50
                       :id "frequency"]
     [:div.slider-wrapper [:input {:type "range"}]
                       :min 1
                       :max 100
                       :value 50
                       :id "amplitude"]
     [:div.patternkeyscontainer
        (for [voice [:kick :snare :closed-hat :open-hat :tom :clap]]
          [pattern-button {:key voice
                           :voice voice
                           :selected? (= voice selected)}
             (name voice)])]
     [:div.keyscontainer
      (for [[kick-val beat] (map list selected-seq (range 0 16))]
        [sequencer-button 
         {:key beat
          :bar (inc (Math/floor (/ beat bars)))
          :on? (= kick-val 1)
          :on-click #(rf/dispatch [(if (= kick-val 1) 
                                     :button-off 
                                     :button-on) 
                                   selected 
                                   beat])}])]]))

(defn mount-root []
  (r/render [app] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:init])
  (mount-root))



















