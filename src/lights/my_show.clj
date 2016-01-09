(ns lights.my-show
  "Set up the fixtures, effects, and cues I actually want to use."
  ;; TODO: Your list of required namespaces will differ from this, depending on
  ;;       what fixtures you actually use, and what effects and cues you create.
  (:require [afterglow.channels :as chan]
            [afterglow.core :as core]
            [afterglow.controllers :as ct]
            [afterglow.transform :as tf]
            [afterglow.effects :as fx]
            [afterglow.effects.color :as color-fx]
            [afterglow.effects.cues :as cues]
            [afterglow.effects.dimmer :refer [dimmer-effect]]
            [afterglow.effects.fun :as fun]
            [afterglow.effects.movement :as move]
            [afterglow.effects.oscillators :as oscillators]
            [afterglow.effects.params :as params]
            [afterglow.effects.show-variable :as var-fx]
            [afterglow.fixtures.blizzard :as blizzard]
            [afterglow.rhythm :as rhythm]
            [afterglow.show :as show]
            [afterglow.show-context :refer :all]
            [com.evocomputing.colors :refer [color-name create-color hue adjust-hue]]
            [taoensso.timbre :as timbre]))

(defonce ^{:doc "Holds my show if it has been created,
  so it can be unregistered if it is being re-created."}
  my-show
  (atom nil))

(defn rgbd-cheap
  "A simple RGB with dimmer and strobe on 1 channel"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/functions :control 4
                0 {:type :dimmer
                  :label "Dimmer from 0 to 189"
                  :range :variable}
                190 {:type :strobe
                  :label "Strobe from 190"})]
   :name "Simple RGB with dimmer"})

(defn rgb-arch
  "A simple RGB light from achitecture"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)]
   :name "Simple RGB"})

(defn rgbd-simple
  "A simple RGBD from London pub"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/functions :dimmer 4)]
    })

(defn rgbw-simple
  "A simple RGBW from London pub"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/color 4 :white)]
    })

(defn jb-systems-sirius-8ch
  "JB SYSTEMS - Lyre Sirius 60W

  The way these fixtures respond to pan and tilt values seems vary. In
  order to be able to get consistent results with a mixed group of
  fixtures, you can override the values of `:pan-center`,
  `:pan-half-circle`, `:tilt-center` and `:tilt-half-circle` which
  used to work by passing in new values using optional keyword
  arguments."
  [& {:keys [pan-center pan-half-circle tilt-center tilt-half-circle]
    :or {:pan-center 86 :pan-half-circle 0 :tilt-center 35 :tilt-half-circle 224}}]

  {:channels [(chan/pan 1)
              (chan/tilt 2)
              (chan/functions :shutter 3 0 "Shutter Closed" 255 "Shutter Open")
              (chan/dimmer 7)]
    :pan-center 86 :pan-half-circle 0 :tilt-center 35 :tilt-half-circle 224}
)

(defn use-my-show
  "Set up the show on the OLA universes it actually needs."
  []
  (set-default-show!
    (swap! my-show (fn [s]
      (when s
        (show/unregister-show s)
        (with-show s (show/stop!)))
      (show/show :universes [1] :description "Accuraten"))))

  (show/patch-fixture! :back-odd-1 (rgbd-cheap) 1 1 :x 1)
  (show/patch-fixture! :back-even-2 (rgbd-cheap) 1 5 :x 2)
  (show/patch-fixture! :back-odd-3 (rgbd-cheap) 1 9 :x 3)
  (show/patch-fixture! :front-1 (rgb-arch) 1 13  :x 3.5)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

(defn use-london-show
  "Show for the London club"
  []
  (set-default-show!
    (swap! my-show (fn [s]
      (when s
        (show/unregister-show s)
        (with-show s (show/stop!)))

      (show/show :universes [1] :description "London"))))

  (show/patch-fixture! :front-1 (rgbd-simple) 1 1   :x 1.6  :y 3.5 :z 2.6)
  (show/patch-fixture! :front-2 (rgbd-simple) 1 17  :x -1.6 :y 3.5 :z 2.6)

  (show/patch-fixture! :scene-side-1 (rgbw-simple) 1 33  :x 0 :y 1.7 :z 1.5)

  (show/patch-fixture! :head-1 (jb-systems-sirius-8ch :pan-center 86 :pan-half-circle 0 :tilt-center 35 :tilt-half-circle 224) 1 49 :x 3.5  :y 7  :z 2.8)
  (show/patch-fixture! :head-2 (jb-systems-sirius-8ch :pan-center 86 :pan-half-circle 0 :tilt-center 35 :tilt-half-circle 224) 1 65 :x 3.5  :y 7  :z 2.8)
  (show/patch-fixture! :head-3 (jb-systems-sirius-8ch :pan-center 86 :pan-half-circle 0 :tilt-center 35 :tilt-half-circle 224) 1 81 :x 3.5  :y 7  :z 2.8)
  (show/patch-fixture! :head-4 (jb-systems-sirius-8ch :pan-center 86 :pan-half-circle 0 :tilt-center 35 :tilt-half-circle 224) 1 97 :x 3.5  :y 7  :z 2.8)

  (show/patch-fixture! :back-1 (rgbd-cheap) 1 140 :x -1.757 :y 0.325 :z 0.3)
  (show/patch-fixture! :back-2 (rgbd-cheap) 1 144 :x -1.057 :y 0.325 :z 0.3)
  (show/patch-fixture! :back-3 (rgbd-cheap) 1 148 :x -0.39  :y 0.325 :z 0.3)
  (show/patch-fixture! :back-4 (rgbd-cheap) 1 152 :x 0.39   :y 0.325 :z 0.3)
  (show/patch-fixture! :back-5 (rgbd-cheap) 1 156 :x 1.057  :y 0.325 :z 0.3)
  (show/patch-fixture! :back-6 (rgbd-cheap) 1 160 :x 1.757  :y 0.325 :z 0.3)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

(use-my-show)

;(use-london-show)

; Logging level set to :info
(core/init-logging)

(defn global-color-effect
  "Make a color effect which affects all lights in the sample show.
  This became vastly more useful once I implemented dynamic color
  parameters. Can include only a specific set of lights by passing
  them with :lights"
  [color & {:keys [include-color-wheels? lights] :or {lights (show/all-fixtures)}}]
  (try
    (let [[c desc] (cond (= (type color) :com.evocomputing.colors/color)
     [color (color-name color)]
     (and (satisfies? params/IParam color)
          (= (params/result-type color) :com.evocomputing.colors/color))
     [color "variable"]
     :else
     [(create-color color) color])]
      (color-fx/color-effect (str "Color: " desc) c lights :include-color-wheels? include-color-wheels?))
    (catch Exception e
      (throw (Exception. (str "Can't figure out how to create color from " color) e)))))

(defn global-dimmer-effect
  "Return an effect that sets all the dimmers in the sample rig.
  Originally this had to be to a static value, but now that dynamic
  parameters exist, it can vary in response to a MIDI mapped show
  variable, an oscillator, or the location of the fixture. You can
  override the default name by passing in a value with :effect-name"
  [level & {:keys [effect-name]}]
  (dimmer-effect level (show/all-fixtures) :effect-name effect-name))


(defn fiat-lux
  "Start simple with a cool blue color from all the lights."
  []
  (show/add-effect! :color (global-color-effect "slateblue" :include-color-wheels? true))
  (show/add-effect! :dimmers (global-dimmer-effect 255))
)

(core/start-web-server 16000 false)
(show/start!)     ; Start sending its DMX frames.

; Reset dimmers to full brightness
(show/add-effect! :dimmers (global-dimmer-effect 255))

; 2DO rewrite to connect multiple devices
(defn midi-help
  "Bind MIDI devices"
  [interface]

  (if (= interface "z1")
    (show/add-midi-control-to-var-mapping
      "Traktor Kontrol Z1 Input" 0 4 :knob-1 :max 360)
  )

  (if (= interface "uno")
    (do
      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 0 :audio-drums :min -10 :max 50)
      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 1 :audio-bass :max 50)
      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 2 :audio-solo :max 50)
      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 3 :audio-voice :min -120 :max 50)
      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 4 :audio-percussion :max 50)
    )
  )

  (if (= interface "audio6")
    (do
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 0 :audio-drums :min -10 :max 50)
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 0 :audio-drums-front :min -10 :max 50
        :transform-fn (fn [v] (* v (afterglow.show/get-variable :lightness-max-front-percent))))
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 1 :audio-bass :max 50)
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 1 :audio-bass-front :max 50
        :transform-fn (fn [v] (* v (afterglow.show/get-variable :lightness-max-front-percent))))
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 2 :audio-solo :max 50)
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 3 :audio-voice :min -120 :max 50)
      (show/add-midi-control-to-var-mapping
        "Komplete Audio 6" 9 4 :audio-percussion :max 50)
    )
  )

  (if (= interface "automap")
    (do
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 21 :sparkle-chance :min 0.01 :max 0.2)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 22 :sparkle-fade :min 1 :max 500)
      (show/add-midi-control-to-cue-mapping "Automap MIDI" 10 :control 51 0 7)
      (show/add-midi-control-to-cue-mapping "Automap MIDI" 10 :control 52 1 7)
      (show/add-midi-control-to-var-mapping "Automap MIDI" 10 66 :osc-beat-ratio :max 3 :transform-fn (fn [v] (Math/pow 2 (Math/round (- 3 v)))))
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 25 :lightness-max-general :min 0 :max 100)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 25 :lightness-max-general-percent :min 0 :max 1)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 26 :lightness-max-general :min 0 :max 100)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 27 :lightness-max-front :min 0 :max 50)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 27 :lightness-max-front-percent :min 0 :max 1)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 24 :lightness-min-general :min 0 :max 100
          :transform-fn (fn [v] (- 0 v)))

      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 23 :main-hue :min 0 :max 360)

      ; Reset beat mapping from MIDI button to a cue
      (show/add-midi-control-to-cue-mapping "Automap MIDI" 10 :control 58 7 7)
    )
  )

  (if (= interface "quneo")
    (do
      ; WIP. Right now this code forces cue into a loop and there is no way back. This cue works until stopped from web interface. After that is does not start anymore.
      (cues/add-midi-control-to-cue-mapping "Bus 1" 0
        :note 36 0 7 :use-velocity? true)
      )
    )

  (if (= interface "live")
    (do
      ; WIP. Right now this code forces cue into a loop and there is no way back. This cue works until stopped from web interface. After that is does not start anymore.
      (show/add-midi-control-to-var-mapping
        "Bus 1" 11 36 :front-1-note :max 255)
      )

      (show/add-midi-control-to-var-mapping
          "Bus 1" 11 37 :main-adjust-hue :max 60)
        )
    )

  (if (= interface "uno-clock")
    (show/sync-to-external-clock
      (afterglow.midi/sync-to-midi-clock "USB Uno MIDI Interface"))
    )

  (if (or (= interface "identify") (= interface "id"))
    (afterglow.midi/identify-mapping))

  (if (= interface "traktor-local")
    (show/sync-to-external-clock (afterglow.midi/sync-to-midi-clock "Traktor Virtual Output"))
  )
)

; Default settings
(afterglow.show/set-variable! :sparkle-chance 0.1)
(afterglow.show/set-variable! :sparkle-fade 100)
(afterglow.show/set-variable! :lightness-min-general 0)
(afterglow.show/set-variable! :lightness-max-front 20)
(afterglow.show/set-variable! :lightness-max-general 50)
(afterglow.show/set-variable! :lightness-max-general-percent 0.5)
(afterglow.show/set-variable! :lightness-max-front-percent 0.2)
(afterglow.show/set-variable! :use-hue-chase false)
(afterglow.show/set-variable! :osc-beat-ratio 8)
(afterglow.show/set-variable! :front-1-note 0)
(afterglow.show/set-variable! :main-adjust-hue 0)

(defn light-sawtooth-phase
  "Change light of fixtures with phase shift. WIP."
  []
  (let [phase-gradient (params/build-spatial-param  ; Spread a phase shift across fixtures
      (show/all-fixtures)
      (fn [head] (- (:x head) (:min-x @(:dimensions *show*)))) :max 1)]
    (let [light-param (params/build-oscillated-param
      (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true :phase phase-gradient) :max :max-lightness)]
      (show/add-effect! :color (global-color-effect
        (params/build-color-param :s 100 :l light-param :h :main-hue))
      )
    )
  )
)

(defn light-sawtooth-color
  "Change light according to sawtooth osc & hue phase"
  ([]
    (light-sawtooth-color (show/all-fixtures))
    )
  ([fixtures]
    (let [light-param (params/build-oscillated-param
      (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true) :max :max-lightness)
          hue-param (params/build-oscillated-param
      (oscillators/square-beat :beat-ratio 16 :down? true) :max 90)
    ]
      (show/add-effect! :color (afterglow.effects.color/color-effect
                          "Light sawtooth color" (params/build-color-param :h :main-hue :s 100 :l light-param :adjust-hue hue-param) fixtures))
    ))
)

(defn light-to-param
  "Change light according to dynamic parameter"
  [param]
  (let [light-param (params/build-oscillated-param
    (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true) :max :max-lightness)
        hue-param (params/build-oscillated-param
    (oscillators/square-beat :beat-ratio 16 :down? true) :max 90)
  ]
    (show/add-effect! :color (global-color-effect
      (params/build-color-param :h :main-hue :s 100 :l param)))
  )
)

(defn shutter-open
  "Open shutters on heads in London show"
  []
  (show/add-effect! :shutter
    (afterglow.effects.channel/channel-effect "Shutter" 255
      (afterglow.channels/extract-channels
        (show/fixtures-named "head") #(= (:type %) :shutter))))
)

(defn calibrate-heads
  "Helper functions to calibrate heads"
  []
  (show/add-effect! :pan-head
    (afterglow.effects.channel/channel-effect
      "Pan" (params/build-variable-param :pan)
      (afterglow.channels/extract-channels
        (show/fixtures-named "head") #(= (:type %) :pan))))
  (show/add-effect! :tilt-head
    (afterglow.effects.channel/channel-effect
      "Tilt" (params/build-variable-param :tilt)
      (afterglow.channels/extract-channels
        (show/fixtures-named "head") #(= (:type %) :tilt))))
  (afterglow.show/set-variable! :pan 130)
  (afterglow.show/set-variable! :tilt 120)
  )

(defn head-direction-z
  "Direct all heads to Z = 1"
  []
  ; (afterglow.effects.movement/direction-effect "Towards z" (params/build-direction-param :x 0 :y 0 :z 1) (show/fixtures-named "head"))

  (show/add-effect! :position
    (move/direction-effect
     "Towards z" (params/build-direction-param :x 0 :y -1 :z 0) (show/fixtures-named "head")))
  )

(defn london-init
  "Init of London show"
  []
  (midi-help "automap")
  (midi-help "audio6")
  (shutter-open)
  (calibrate-heads)
  )

;(london-init)

(fiat-lux)

(defn head-aim-center
  "Direct head to the center of dancefloor"
  []
  (show/add-effect! :position
    (move/aim-effect "Center" (params/build-aim-param :x 0 :y 0 :z 5) (show/fixtures-named "head")))
)

; Magic from Afterglow examples
(defonce ^{:doc "Allows effects to set variables in the running show."}
  var-binder
  (atom nil))
(reset! var-binder (var-fx/create-for-show *show*))

(defn new-cues
  "Create cues."
  []

  (ct/set-cue! (:cue-grid *show*) 0 7
    (cues/cue :sparkle (fn [_] (afterglow.effects/scene "Sparkle all"
        (afterglow.effects.fun/sparkle (show/fixtures-named "back") :chance :sparkle-chance :fade-time :sparkle-fade)
        (afterglow.effects.fun/sparkle (show/fixtures-named "front") :chance :sparkle-chance :fade-time :sparkle-fade)
        (afterglow.effects.fun/dimmer-sparkle (show/fixtures-named "head") :chance :sparkle-chance :fade-time :sparkle-fade)))
        :held true :priority 100
        :short-name "Sparkle all"
        ))

  (ct/set-cue! (:cue-grid *show*) 7 7
    (cues/code-cue (fn [show snapshot]
      (rhythm/metro-start (:metronome show) 1))
        "Reset"))

  (ct/set-cue! (:cue-grid *show*) 0 6
    (cues/cue :dimmers (fn [_] (dimmer-effect 255 (show/all-fixtures) :add-virtual-dimmers? true :htp? false))
        :short-name "All dimmers up"
        ))

  (ct/set-cue! (:cue-grid *show*) 1 6
    (cues/cue :dimmers (fn [_] (dimmer-effect 0 (show/all-fixtures) :add-virtual-dimmers? true :htp? false))
        :short-name "All dimmers down"
        ))

  (ct/set-cue! (:cue-grid *show*) 2 6
    (cues/cue :dimmers (fn [_]
      (afterglow.effects/scene
        (dimmer-effect 0 (show/fixtures-named "head") :add-virtual-dimmers? true :htp? false)
        (dimmer-effect 255 (show/fixtures-named "front") :add-virtual-dimmers? true :htp? false)
        (dimmer-effect 255 (show/fixtures-named "back") :add-virtual-dimmers? true :htp? false)
        )
      )
    :short-name "Back & front"
    ))

  (ct/set-cue! (:cue-grid *show*) 3 6
    (cues/cue :dimmers (fn [_]
      (afterglow.effects/scene
        (dimmer-effect 0 (show/fixtures-named "head") :add-virtual-dimmers? true :htp? false)
        (dimmer-effect 0 (show/fixtures-named "front") :add-virtual-dimmers? true :htp? false)
        (dimmer-effect 255 (show/fixtures-named "back") :add-virtual-dimmers? true :htp? false)
        )
      )
    :short-name "Back only"
    ))

  (ct/set-cue! (:cue-grid *show*) 4 6
    (cues/cue :dimmers (fn [_]
      (afterglow.effects/scene
        (dimmer-effect 255 (show/fixtures-named "front") :add-virtual-dimmers? true :htp? false)
        (dimmer-effect 255 (show/fixtures-named "back") :add-virtual-dimmers? true :htp? false)
        )
      )
    :short-name "TEST 255"
    ))

  (ct/set-cue! (:cue-grid *show*) 7 6
    (cues/cue :dimmers (fn [_]
      (afterglow.effects/scene
        (dimmer-effect 0 (show/fixtures-named "back") :add-virtual-dimmers? true :htp? false)
        (dimmer-effect (params/build-variable-param :front-1-note) (show/fixtures-named "front") :add-virtual-dimmers? true :htp? false)
        )
      )
    :short-name "MIDI controlled"
    ))

  (ct/set-cue! (:cue-grid *show*) 0 4
    (cues/cue :position  (fn [_] (afterglow.effects/scene
      "Change heads directions"
      (fx/chase "Chase heads directions" [
        (move/direction-effect "" (params/build-direction-param :x 0 :y 0 :z 1) (show/fixtures-named "head"))
        (move/direction-effect "" (params/build-direction-param :x 1 :y 0 :z 0) (show/fixtures-named "head"))
        (move/direction-effect "" (params/build-direction-param :x 0 :y 0 :z -1) (show/fixtures-named "head"))
        (move/direction-effect "" (params/build-direction-param :x 0 :y -1 :z 0) (show/fixtures-named "head"))
        (move/direction-effect "" (params/build-direction-param :x -1 :y 0 :z 0) (show/fixtures-named "head"))
        ]
        (params/build-step-param :interval :beat :fade-fraction 0.3 :fade-curve :sine)
        :beyond :loop
        )))))

  (ct/set-cue! (:cue-grid *show*) 1 4
    (cues/cue :position  (fn [_] (afterglow.effects/scene
      "Heads corners of dancefloor"
      (fx/chase "Heads corners of dancefloor" [
        (move/aim-effect "" (params/build-aim-param :x 0 :y 0 :z 0) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x 5 :y 0 :z 0) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x -5 :y 0 :z 0) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x -5 :y 0 :z 5) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x 0 :y 0 :z 5) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x 5 :y 0 :z 5) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x 5 :y 0 :z 10) (show/fixtures-named "head"))
        (move/aim-effect "" (params/build-aim-param :x -5 :y 0 :z 10) (show/fixtures-named "head"))
        ]
        (params/build-step-param :interval :beat :fade-fraction 0.3 :fade-curve :sine)
        :beyond :loop
        )))))

  (ct/set-cue! (:cue-grid *show*) 0 3
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "All drums"
        (afterglow.effects.color/color-effect
          "Drums" (params/build-color-param :h :main-hue :s 100 :l :audio-drums) (show/fixtures-named "back"))
        (afterglow.effects.color/color-effect
          "Bass" (params/build-color-param :h :main-hue :s 100 :l :audio-drums-front) (show/fixtures-named "front"))
    ))))
  (ct/set-cue! (:cue-grid *show*) 1 3
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "All bass"
        (afterglow.effects.color/color-effect
          "Bass" (params/build-color-param :h :main-hue :s 100 :l :audio-bass) (show/fixtures-named "back"))
        (afterglow.effects.color/color-effect
          "Bass" (params/build-color-param :h :main-hue :s 100 :l :audio-bass-front) (show/fixtures-named "front"))
    ))))
  (ct/set-cue! (:cue-grid *show*) 2 3
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Drums back, bass front"
        (afterglow.effects.color/color-effect
          "Drums" (params/build-color-param :h :main-hue :s 100 :l :audio-drums) (show/fixtures-named "back"))
        (afterglow.effects.color/color-effect
          "Bass" (params/build-color-param :h :main-hue :s 100 :l :audio-bass-front) (show/fixtures-named "front"))
    ))))

  (let [light-param (params/build-oscillated-param
        (oscillators/sawtooth :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-back)]
    (ct/set-cue! (:cue-grid *show*) 0 2
      (cues/cue :color  (fn [_] (afterglow.effects/scene
        "Sawtooth Back"
          (afterglow.effects.color/color-effect
            "Bass" (params/build-color-param :h :main-hue :s 100 :l light-param) (show/fixtures-named "back"))
      )))))


  (let [light-param (params/build-oscillated-param
        (oscillators/sawtooth :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-front)]
    (ct/set-cue! (:cue-grid *show*) 1 2
      (cues/cue :color  (fn [_] (afterglow.effects/scene
        "Sawtooth Front"
          (afterglow.effects.color/color-effect
            "Bass" (params/build-color-param :h :main-hue :s 100 :l light-param) (show/fixtures-named "front"))
      )))))

  (let [light-param-back (params/build-oscillated-param
        (oscillators/sawtooth :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-back)
        light-param-front (params/build-oscillated-param
        (oscillators/sawtooth :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-front)]
    (ct/set-cue! (:cue-grid *show*) 2 2
      (cues/cue :color  (fn [_] (afterglow.effects/scene
        "Sawtooth Back & Front"
          (afterglow.effects.color/color-effect
            "Back" (params/build-color-param :h :main-hue :s 100 :l light-param-back) (show/fixtures-named "back"))
          (afterglow.effects.color/color-effect
            "Front" (params/build-color-param :h :main-hue :s 100 :l light-param-front) (show/fixtures-named "front"))
      )))))

  (ct/set-cue! (:cue-grid *show*) 1 2
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Sine Back & Front"
      (let [light-param (params/build-oscillated-param
        (oscillators/sine :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-general)]
        (global-color-effect (params/build-color-param :h :main-hue :s 100 :l light-param))
      )
    ))))

  (ct/set-cue! (:cue-grid *show*) 0 1
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Sawtooth All"
      (let [light-param (params/build-oscillated-param
        (oscillators/sawtooth :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-general)]
        (global-color-effect (params/build-color-param :h :main-hue :s 100 :l light-param))
      )
    ))))

  (ct/set-cue! (:cue-grid *show*) 1 1
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Sine All"
      (let [light-param (params/build-oscillated-param
        (oscillators/sine :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-general)]
        (global-color-effect (params/build-color-param :h :main-hue :s 100 :l light-param))
      )
    ))))

  (ct/set-cue! (:cue-grid *show*) 2 1
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Saw all, change colors 16B"
      (let [light-param (params/build-oscillated-param
        (oscillators/sawtooth :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-general)]
        (fx/chase "Saw all, hue adjust" [(global-color-effect (params/build-color-param :h :main-hue :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 30 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 60 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 90 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 120 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 150 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 180 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 210 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 240 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 270 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 300 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 330 :s 100 :l light-param))]
          (params/build-step-param :interval :phrase :fade-fraction 0.3 :fade-curve :sine)
          :beyond :loop
          ))))))

  (ct/set-cue! (:cue-grid *show*) 3 1
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Solid with adjust"
        (global-color-effect (params/build-color-param :h :main-hue :adjust-hue :main-adjust-hue :s 100 :l 50))
    ))))

  (ct/set-cue! (:cue-grid *show*) 3 1
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Sine all, change colors 16B"
      (let [light-param (params/build-oscillated-param
        (oscillators/sine :interval :beat :interval-ratio :osc-beat-ratio :down? true) :min :lightness-min-general :max :lightness-max-general)]
        (fx/chase "Saw all, hue adjust" [(global-color-effect (params/build-color-param :h :main-hue :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 30 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 60 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 90 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 120 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 150 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 180 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 210 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 240 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 270 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 300 :s 100 :l light-param))
          (global-color-effect (params/build-color-param :h :main-hue :adjust-hue 330 :s 100 :l light-param))]
          (params/build-step-param :interval :phrase :fade-fraction 0.3 :fade-curve :sine)
          :beyond :loop
          ))))))

  (ct/set-cue! (:cue-grid *show*) 0 0
    (cues/cue :set-main-hue (fn [_] (var-fx/variable-effect @var-binder :main-hue 0)) 
      :short-name "Red"
      )
    )
  (ct/set-cue! (:cue-grid *show*) 1 0
    (cues/cue :set-main-hue (fn [_] (var-fx/variable-effect @var-binder :main-hue 60)) 
      :short-name "Yellow"
      )
    )
  (ct/set-cue! (:cue-grid *show*) 2 0
    (cues/cue :set-main-hue (fn [_] (var-fx/variable-effect @var-binder :main-hue 120)) 
      :short-name "Green"
      )
    )
  (ct/set-cue! (:cue-grid *show*) 3 0
    (cues/cue :set-main-hue (fn [_] (var-fx/variable-effect @var-binder :main-hue 180))
      :short-name "Blue"
      )
    )
  (ct/set-cue! (:cue-grid *show*) 4 0
    (cues/cue :set-main-hue (fn [_] (var-fx/variable-effect @var-binder :main-hue 240))
      :short-name "Dark blue"
      )
    )
  (ct/set-cue! (:cue-grid *show*) 5 0
    (cues/cue :set-main-hue (fn [_] (var-fx/variable-effect @var-binder :main-hue 300))
      :short-name "Purple"
      )
    )
  (ct/set-cue! (:cue-grid *show*) 6 0
    (cues/cue :set-main-hue (fn [_] (fx/blank))
      :short-name "MIDI"
      )
    )
)

(new-cues)