(ns harmony.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn app []
  [:h1 "Harmony"])

(defn mount-root []
  (r/render [app] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
