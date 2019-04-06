(ns harmony.overtone
  (:require
   [overtone.live :as overtone :refer [metronome at apply-by]]
   [overtone.studio.inst :refer [definst]]
   [overtone.sc.ugens :refer [bpf lpf white-noise line FREE env-gen]]
   [overtone.sc.envelope :refer [envelope]]
   [overtone.inst.drum :refer [kick closed-hat open-hat tom snare]]))

(definst clap
  [vol {:default 1 :min 0 :max 1 :step 0.001}
   low {:default 7500 :min 100 :max 10000 :step 1}
   hi  {:default 1500 :min 100 :max 10000 :step 1}
   amp {:default 0.3 :min 0.001 :max 1 :step 0.01}
   decay {:default 0.6 :min 0.1 :max 0.8 :step 0.001}]
  (let [noise      (bpf (lpf (white-noise) low) hi)
        clap-env   (line vol 0 decay :action FREE)
        noise-envs (map #(envelope [0 0 1 0] [(* % 0.01) 0 0.04]) (range 8))
        claps      (apply + (* noise (map env-gen noise-envs)))]
    (* claps clap-env)))

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
               :snare (empty-sequence)}
   :parameters {:kick {:frequency 20 :volume 50}
                :open-hat {:frequency 50 :volume 50}
                :closed-hat {:frequency 50 :volume 50}
                :tom {:frequency 50 :volume 50}
                :clap {:frequency 50 :volume 50}
                :snare {:frequency 50 :volume 50}}})

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
    (schedule-voice :kick first-beat nome
                    #(kick (+ 30 (get-in @db [:parameters :kick :frequency]))))
    (schedule-voice :open-hat first-beat nome #(open-hat))
    (schedule-voice :closed-hat first-beat nome #(closed-hat))
    (schedule-voice :tom first-beat nome #(tom))
    (schedule-voice :clap first-beat nome
                    #(clap (/ (dec (get-in @db [:parameters :clap :volume])) 100)))
    (schedule-voice :snare first-beat nome #(snare))
    (apply-by (nome next-first-beat) #'sequence-loop [nome])))

(sequence-loop nome)

(comment
  (overtone/stop-all)

  (sequence-loop nome)

  (reset! db initial-db)

  @db
  )
