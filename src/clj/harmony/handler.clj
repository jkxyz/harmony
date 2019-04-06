(ns harmony.handler
  (:require
   [compojure.core :refer [routes GET]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.reload :refer [wrap-reload]]
   [prone.middleware :refer [wrap-exceptions]]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]
   [org.httpkit.server :refer [with-channel on-close on-receive send!]]
   [harmony.overtone :as overtone]
   [clojure.edn :as edn])
  (:import java.util.UUID))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, user-scalable=no"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    [:div#app [:h2 "Loading..."]]
    (include-js "/js/app.js")]))

(defn index-handler []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(defonce clients (atom {}))

(defmulti dispatch first)

(defmethod dispatch :button-on
  [[_ voice index]]
  (swap! overtone/db assoc-in [:sequences voice index] 1))

(defmethod dispatch :button-off
  [[_ voice index]]
  (swap! overtone/db assoc-in [:sequences voice index] 0))

(defmethod dispatch :set-param
  [[_ voice param value]]
  (swap! overtone/db assoc-in [:parameters voice param] (Integer/parseInt value)))

(defn broadcast [[event & rest]]
  (doseq [ch (vals @clients)]
    (send! ch (str (into [(keyword "server" (name event))] rest)))))

(defn websocket-handler [request]
  (with-channel request ch
    (let [client-id (UUID/randomUUID)]
      (swap! clients assoc client-id ch)
      (send! ch (str [:server/init @overtone/db]))
      (on-close ch (fn [_] (swap! clients dissoc client-id)))
      (on-receive
       ch
       (fn [m]
         (let [event (edn/read-string m)]
           (println "Received:" event)
           (dispatch event)
           (broadcast event)))))))

(comment
  @clients
  (reset! clients {})
  )

(def app
  (-> (routes
       (GET "/" [] (index-handler))
       (GET "/ws" request (websocket-handler request)))
      (wrap-defaults site-defaults)
      (wrap-exceptions)
      (wrap-reload)))
