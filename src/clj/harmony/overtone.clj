(ns harmony.overtone
  (:require
   [overtone.live :as overtone :refer [metronome at apply-by]]
   [overtone.inst.drum :refer [kick]]))

(def sequence-length 16)

(def bars 4)

(defn empty-sequence [] (into [] (repeat sequence-length 0)))

(def initial-db
  {:sequences {:kick (empty-sequence)}})

(defonce db (atom initial-db))

(defonce nome (metronome 140))

(defn beat-offset [index]
  (+ (Math/floor (/ index 4)) (/ (mod index 4) 4)))


(defn schedule-kick [first-beat nome]
  (dotimes [index sequence-length]
    (when-not (= 0 (get (-> @db :sequences :kick) index))
      (at (nome (+ first-beat (beat-offset index))) (kick)))))

(defn sequence-loop [nome]
  (let [first-beat (nome)
        next-first-beat (+ first-beat 4)]
    (println "Loop start:" first-beat)
    (schedule-kick first-beat nome)
    (apply-by (nome next-first-beat) #'sequence-loop [nome])))

(comment
  (overtone/stop-all)

  (sequence-loop nome)

  (swap! db assoc :kick [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1])
  (swap! db assoc :kick [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0])

  (reset! db initial-db)

  @db
  )
