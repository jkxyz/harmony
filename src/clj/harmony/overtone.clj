(ns harmony.overtone
  (:require
   [overtone.live :as overtone :refer [metronome at apply-by]]
   [overtone.inst.drum :refer [kick closed-hat open-hat tom clap snare]]))

(def sequence-length 16)

(def bars 4)

(defn empty-sequence [] (into [] (repeat sequence-length 0)))

(def initial-db
  {:bpm 140
   :sequences {:kick (empty-sequence)
               :open-hat (empty-sequence)
               :closed-hat (empty-sequence)
               :tom (empty-sequence)
               :clap (empty-sequence)
               :snare (empty-sequence)}})

(defonce db (atom initial-db))

(defonce nome (metronome 140))

(defn beat-offset [index]
  (+ (Math/floor (/ index 4)) (/ (mod index 4) 4)))

(defn schedule-voice [voice first-beat nome f]
  (dotimes [index sequence-length]
    (when (= 1 (get-in @db [:sequences voice index]))
      (at (nome (+ first-beat (beat-offset index))) (f)))))

(defn sequence-loop [nome]
  (let [first-beat (nome)
        next-first-beat (+ first-beat 4)]
    (println "Loop start:" first-beat)
    (schedule-voice :kick first-beat nome #(kick))
    (schedule-voice :open-hat first-beat nome #(open-hat))
    (schedule-voice :closed-hat first-beat nome #(closed-hat))
    (schedule-voice :tom first-beat nome #(tom))
    (schedule-voice :clap first-beat nome #(clap))
    (schedule-voice :snare first-beat nome #(snare))
    (apply-by (nome next-first-beat) #'sequence-loop [nome])))

(sequence-loop nome)

(comment
  (overtone/stop-all)

  (sequence-loop nome)

  (reset! db initial-db)

  @db
  )
