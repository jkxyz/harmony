(ns harmony.overtone
  (:require
   [overtone.live :as overtone :refer [metronome at apply-by]]
   [overtone.studio.inst :refer [definst]]
   [overtone.sc.ugens :refer [bpf lpf white-noise line FREE env-gen sin-osc]]
   [overtone.sc.envelope :refer [envelope perc]]
   [overtone.inst.drum :as drum :refer [closed-hat open-hat tom snare]]))

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

(definst kick
  [freq       {:default 50 :min 40 :max 140 :step 1}
   vol        {:default 1.0 :min 0.0 :max 1.0 :step 0.001}
   env-ratio  {:default 3 :min 1.2 :max 8.0 :step 0.1}
   freq-decay {:default 0.02 :min 0.001 :max 1.0 :step 0.001}
   amp-decay  {:default 0.5 :min 0.001 :max 1.0 :step 0.001}]
  (let [fenv (* (env-gen (envelope [env-ratio 1] [freq-decay] :exp)) freq)
        aenv (env-gen (perc 0.005 amp-decay) :level-scale vol :action FREE)]
    (* (sin-osc fenv (* 0.5 Math/PI)) aenv)))

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
   :parameters {:kick {:frequency 20 :volume 100}
                :open-hat {:filter 2000 :volume 30}
                :closed-hat {:decay 10 :volume 30}
                :tom {:frequency 90 :volume 50}
                :clap {:filter 1500 :volume 50}
                :snare {:frequency 50 :volume 30}}})

(defonce db (atom initial-db))

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
                    #(kick (+ 30 (get-in @db [:parameters :kick :frequency]))
                           (/ (dec (get-in @db [:parameters :kick :volume])) 100)))
    (schedule-voice :open-hat first-beat nome
                    #(open-hat
                      :amp (/ (get-in @db [:parameters :open-hat :volume]) 100)
                      :hi (get-in @db [:parameters :open-hat :filter])))
    (schedule-voice :closed-hat first-beat nome
                    #(closed-hat (/ (dec (get-in @db [:parameters :closed-hat :volume])) 100)
                                 (/ (get-in @db [:parameters :closed-hat :decay]) 100)))
    (schedule-voice :tom first-beat nome
                    #(tom
                      :freq (get-in @db [:parameters :tom :frequency])
                      :amp (/ (get-in @db [:parameters :tom :volume]) 100)))
    (schedule-voice :clap first-beat nome
                    #(clap (/ (dec (get-in @db [:parameters :clap :volume])) 100)
                           (get-in @db [:parameters :clap :filter])))
    (schedule-voice :snare first-beat nome
                    #(snare (get-in @db [:parameters :snare :frequency])
                            (/ (get-in @db [:parameters :snare :volume]) 100)))
    (apply-by (nome next-first-beat) #'sequence-loop [nome])))

(defn start! []
  (sequence-loop (metronome 140)))

(defn stop! []
  (overtone/stop-all))

(comment
  (start!)
  (stop!)
  (reset! db initial-db)
  @db
  )
