(ns harmony.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn button [{:keys [bar on?]}]
  [:div
   {:class ["keybutton"
            (if (even? bar) "keybutton-pink" "keybutton-purple")
            (when on? "keybutton-pressed")]}
   [:div.button-light]])

(defn tempo [bpm]
  [:div.tempo-display bpm])

(defn app []
  (fn []
    [:span.main
     [:h1 "h a r m o n y"]
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
     [:div.tempocontainer [tempo 50]] ; dummy value
     [:div.keyscontainer
      [button {:bar 1 :on? false}] 
      [button {:bar 1 :on? false}] 
      [button {:bar 1 :on? false}] 
      [button {:bar 1 :on? false}] 
      [button {:bar 2 :on? false}] 
      [button {:bar 2 :on? false}] 
      [button {:bar 2 :on? false}] 
      [button {:bar 2 :on? false}] 
      [button {:bar 3 :on? true}] 
      [button {:bar 3 :on? false}] 
      [button {:bar 3 :on? false}] 
      [button {:bar 3 :on? false}] 
      [button {:bar 4 :on? false}] 
      [button {:bar 4 :on? false}] 
      [button {:bar 4 :on? false}] 
      [button {:bar 4 :on? false}]]])) 
     


(defn mount-root []
  (r/render [app] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
