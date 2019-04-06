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
 (fn [_ [_ initial-db]]
   initial-db))

(rf/reg-event-db
 :server/button-on
 (fn [db [_ voice beat-index]]
   (assoc-in db [:sequences voice beat-index] 1)))

(rf/reg-event-db
 :server/button-off
 (fn [db [_ voice beat-index]]
   (assoc-in db [:sequences voice beat-index] 0)))

;; UI EVENTS

(rf/reg-event-fx
 :button-on
 (fn [{:keys [db]} [_ voice beat-index]]
   {:send [:button-on voice beat-index]}))

(rf/reg-event-fx
 :button-off
 (fn [{:keys [db]} [_ voice beat-index]]
   {:send [:button-off voice beat-index]}))

;; SUBSCRIPTIONS

(rf/reg-sub
 ::sequence
 (fn [db [_ voice]]
   (-> db :sequences voice)))

;; COMPONENTS

(defn sequencer-button [{:keys [bar on? on-click]}]
  [:div
   {:on-click on-click
    :class ["keybutton"
            (if (even? bar) "keybutton-pink" "keybutton-purple")
            (when on? "keybutton-pressed")]}
   [:div.button-light]])

(defn pattern-button [{:keys [instrument selected?]}]
  [:div
   {:class ["instrument-button"
            (when selected? "instrument-button-pressed")]}
   instrument])

(defn tempo [bpm]
  [:div.tempocontainer [:div.tempo-display bpm]])

(defn app []
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
    [pattern-button {:instrument "KICK" :selected? true}]
    [pattern-button {:instrument "SNARE" :selected? true}]
    [pattern-button {:instrument "CLOSED-HAT" :selected? true}]
    [pattern-button {:instrument "OPEN-HAT" :selected? true}]
    [pattern-button {:instrument "TOM" :selected? true}]
    [pattern-button {:instrument "CLAP" :selected? true}]]
   [:div.keyscontainer
    (for [[kick-val beat] (map list @(rf/subscribe [::sequence :kick]) (range 0 16))]
      [sequencer-button 
       {:key beat
        :bar (inc (Math/floor (/ beat bars)))
        :on? (= kick-val 1)
        :on-click #(rf/dispatch [(if (= kick-val 1) ::button-off ::button-on) :kick beat])}])]])

(defn mount-root []
  (r/render [app] (.getElementById js/document "app")))

(defn init! []
  (mount-root))



















