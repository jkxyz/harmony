(ns harmony.overtone
  (:require
   [overtone.live :as overtone :refer [metronome at apply-by]]
   [overtone.inst.drum :refer [kick]]))

(def sequence-length 16)

(defn empty-sequence [] (into [] (repeat sequence-length 0)))

(def initial-db
  {:voices {:kick (empty-sequence)}})

(defonce db (atom initial-db))

(defonce nome (metronome 140))

(defn schedule-kick [first-beat nome]
  (dotimes [beat-offset sequence-length]
    (when-not (= 0 (get (-> @db :voices :kick) beat-offset))
      (at (nome (+ first-beat beat-offset)) (kick)))))

(defn sequence-loop [nome]
  (let [first-beat (nome)
        next-first-beat (+ first-beat sequence-length)]
    (println "Loop start:" first-beat)
    (schedule-kick first-beat nome)
    (apply-by (nome next-first-beat) #'sequence-loop [nome])))

(comment
  (overtone/stop-all)

  (sequence-loop nome)

  (swap! db assoc :kick [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1])
  (swap! db assoc :kick [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0])

  (reset! db initial-db)
  )
