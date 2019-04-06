(ns harmony.handler
  (:require
   [compojure.core :refer [routes GET]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.reload :refer [wrap-reload]]
   [prone.middleware :refer [wrap-exceptions]]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]
   [overtone.core :as overtone]))

(def mount-target
  [:div#app
   [:h2 "Loading..."]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")]))

(defn index-handler []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(def app
  (-> (routes (GET "/" [] (index-handler)))
      (wrap-defaults site-defaults)
      (wrap-exceptions)
      (wrap-reload)))
