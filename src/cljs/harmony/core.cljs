(ns harmony.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.edn :as edn]))

(def sequence-length 16)

(def bars 4)

(defn empty-sequence []
  (into [] (repeat sequence-length 0)))

(defonce conn (js/WebSocket. (str "ws://" js/window.location.hostname ":3449/ws")))

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
   (println "Sending:" (str event))
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

(rf/reg-event-db
 :server/set-param
 (fn [db [_ voice param value]]
   (assoc-in db [:server :parameters voice param] value)))

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

(rf/reg-event-fx
 :set-param
 (fn [_ [_ voice param value]]
   {:send [:set-param voice param value]}))

(rf/reg-event-fx
  :start
  (fn [_ _]
    {:send [:start]}))

(rf/reg-event-fx
  :stop
  (fn [_ _]
    {:send [:stop]}))

;; SUBSCRIPTIONS

(rf/reg-sub
 ::sequence
 (fn [db [_ voice]]
   (-> db :server :sequences voice)))

(rf/reg-sub
 ::selected-voice
 (fn [db _]
   (:selected-voice db)))

(rf/reg-sub
 ::bpm
 (fn [db _]
   (-> db :server :bpm)))

(rf/reg-sub
 ::param
 (fn [db [_ voice param]]
   (get-in db [:server :parameters voice param])))

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
  [:div.tempocontainer
   [:div.tempo-display
    @(rf/subscribe [::bpm])]])

(defn slider [{:keys [voice param text min max] :or {min 0 max 100}}]
  (let [input (atom nil)
        param-val (rf/subscribe [::param voice param])]
    (r/create-class
     {:component-did-update
      (fn []
        (set! (.-value @input) @param-val))
      :component-did-mount
      (fn []
        (set! (.-value @input) @param-val))
      :reagent-render
      (fn [{:keys [voice param text]}]
        @param-val
        [:div.slider-container
         [:input.slider
          {:ref (partial reset! input)
           :on-change #(rf/dispatch [:set-param voice param (.-target.value %)])
           :type "range"
           :orient "vertical"
           :min min
           :max max}]
         [:div.slider-text text]])})))

(defn app []
  (let [selected @(rf/subscribe [::selected-voice])
        selected-seq @(rf/subscribe[::sequence selected])]
    [:div.main
     [:h1 [:em "JJ-808 "] [:small "v.1"]]
     [:div.header
      [tempo]
      [:div.start-stop-button-container
       {:on-click #(rf/dispatch [:start])}
       [:div.start-stop-button-text "start"]]
      [:div.start-stop-button-container
       {:on-click #(rf/dispatch [:stop])}
       [:div.start-stop-button-text "stop"]]]
     [:div.sliders-container
      [:div.slider-pair-container
       [slider {:voice :kick :param :frequency :text "Freq."}]
       [slider {:voice :kick :param :volume :text "Vol."}]]
      [:div.slider-pair-container
       [slider {:voice :snare :param :frequency :text "Freq." :min 405 :max 1000}]
       [slider {:voice :snare :param :volume :text "Vol."}]]
      [:div.slider-pair-container
       [slider {:voice :closed-hat :param :decay :text "Decay" :min 1}]
       [slider {:voice :closed-hat :param :volume :text "Vol."}]]
      [:div.slider-pair-container
       [slider {:voice :open-hat :param :filter :text "Filter" :min 1000 :max 8000}]
       [slider {:voice :open-hat :param :volume :text "Vol."}]]
      [:div.slider-pair-container
       [slider {:voice :tom :param :frequency :text "Freq." :min 50 :max 400}]
       [slider {:voice :tom :param :volume :text "Vol."}]]
      [:div.slider-pair-container
       [slider {:voice :clap :param :filter :text "Filter" :min 100 :max 10000}]
       [slider {:voice :clap :param :volume :text "Vol."}]]]
     [:div.patternkeyscontainer
      (for [voice [:kick :snare :closed-hat :open-hat :tom :clap]]
        [pattern-button
         {:key voice :voice voice :selected? (= voice selected)}
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
