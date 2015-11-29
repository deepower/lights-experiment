(ns lights.my-show
  "Set up the fixtures, effects, and cues I actually want to use."
  ;; TODO: Your list of required namespaces will differ from this, depending on
  ;;       what fixtures you actually use, and what effects and cues you create.
  (:require [afterglow.channels :as chan]
            [afterglow.core :as core]
            [afterglow.controllers :as ct]
            [afterglow.transform :as tf]
            [afterglow.effects.color :as color-fx]
            [afterglow.effects.cues :as cues]
            [afterglow.effects.dimmer :refer [dimmer-effect]]
            [afterglow.effects.fun :as fun]
            [afterglow.effects.movement :as move]
            [afterglow.effects.oscillators :as oscillators]
            [afterglow.effects.params :as params]
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


; 2DO: understand how to pack dimmer and strobe into 1 function definition
(defn my-rgbw
  "A simple RGB with dimmer"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/functions :dimmer 4
                0 {:type :dimmer
                  :label "Dimmer from 0 to 189"
                  :start 0
                  :end 189
                  :range :variable}
                190 {:type :strobe
                  :label "Strobe from 190 to 255"
                  :start 190
                  :end 255
                  :range :variable})]
   :name "Simple RGB with dimmer"})

(defn my-rgb
  "A simple RGB light"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)]
   :name "Simple RGB"})

(defn use-my-show
  "Set up the show on the OLA universes it actually needs."
  []

  ;; Create, or re-create the show. Make it the default show so we don't
  ;; need to wrap everything below in a (with-show sample-show ...) binding.
  (set-default-show!
    (swap! my-show (fn [s]
      (when s
        (show/unregister-show s)
        (with-show s (show/stop!)))
      ;; TODO: Edit this to list the actual OLA universe(s) that
      ;;       your show needs to use if they are different than
      ;;       just universe 1, as below, and change the description
      ;;       to something descriptive and in your own style:
      (show/show :universes [1] :description "Deepower"))))

  (show/patch-fixture! :rgbw-1 (my-rgbw) 1 1 :x 1)
  (show/patch-fixture! :rgbw-2 (my-rgbw) 1 5 :x 2)
  (show/patch-fixture! :rgbw-3 (my-rgbw) 1 9 :x 3)
  (show/patch-fixture! :rgb-1 (my-rgb) 1 13  :x 3.5)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

(use-my-show)  ; Set up my show as the default show, using the function above.

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
  (show/add-effect! :torrent-shutter
                    (afterglow.effects.channel/function-effect
                     "Torrents Open" :shutter-open 50 (show/fixtures-named "torrent"))))

(core/start-web-server 16000 true)
(show/start!)     ; Start sending its DMX frames.

; Dim all PARs to maximum
(show/add-effect! :dimmers (global-dimmer-effect 189))

; Variable to control lightness level on all PARs
(show/set-variable! :lightness-level 30)
(show/add-effect! :color (global-color-effect
  (params/build-color-param :h 60 :s 100 :l :lightness-level)))

; Oscillation of hue, temporary removed
;(def hue-param (params/build-oscillated-param
;                 (oscillators/sawtooth-bar) :max 360))

(defn bind-midi
  "Bind MIDI devices"
  []
  (show/add-midi-control-to-var-mapping
    "Traktor Kontrol Z1 Input" 0 4 :knob-1 :max 360)

  (show/add-midi-control-to-var-mapping
    "midi-net" 1 1 :audio-drums :max 30)

  (show/add-midi-control-to-var-mapping
    "midi-net" 1 2 :audio-bass :max 30)

  (show/add-midi-control-to-var-mapping
    "midi-net" 1 3 :audio-percussion :min 10 :max 30)

  (show/add-midi-control-to-var-mapping
    "midi-net" 1 5 :audio-solo :max 30)
)

(defn light-sawtooth
  "Change light according to sawtooth osc"
  []
  (def light-param (params/build-oscillated-param
                 (oscillators/sawtooth-beat :beat-ratio 2 :down true) :max 20))
  (show/add-effect! :color (global-color-effect
    (params/build-color-param :h 60 :s 100 :l light-param)))
  )

(def light-beat (params/build-oscillated-param
                 (oscillators/square-beat) :max 20))

(defn sync-midi-clock
  "Sync MIDI clock to Ableton live session over network"
  []
  (show/sync-to-external-clock
    (afterglow.midi/sync-to-midi-clock "Network midi-net"))
  )

(defn blue-on-one
  "Assing blue color to a big PAR"
  []
  (show/add-effect! :color (afterglow.effects.color/color-effect "simple-blue" (params/build-color-param :h 120 :s 100 :l light-param) (show/fixtures-named "rgb-1"))))

(defn global-color-cue
  "Create a cue-grid entry which establishes a global color effect."
  [color x y & {:keys [include-color-wheels? held]}]
  (let [cue (cues/cue :color (fn [_] (global-color-effect color :include-color-wheels? include-color-wheels?))
                      :held held
                      :color (create-color color))]
    (ct/set-cue! (:cue-grid *show*) x y cue)))

(defn make-cues
  "Create cues."
  []
    (let [hue-bar (params/build-oscillated-param  ; Spread a rainbow across a bar of music
                 (oscillators/sawtooth-bar) :max 360)
        desat-beat (params/build-oscillated-param  ; Desaturate a color as a beat progresses
                    (oscillators/sawtooth-beat :down? true) :max 100)
        hue-gradient (params/build-spatial-param  ; Spread a rainbow across the light grid
                      (show/all-fixtures)
                      (fn [head] (- (:x head) (:min-x @(:dimensions *show*)))) :max 360)
        hue-z-gradient (params/build-spatial-param  ; Spread a rainbow across the light grid front to back
                        (show/all-fixtures)
                        (fn [head] (- (:z head) (:min-z @(:dimensions *show*)))) :max 360)]

  (global-color-cue "red" 0 0 :include-color-wheels? true)
  (global-color-cue "orange" 1 0 :include-color-wheels? true)
  (global-color-cue "yellow" 2 0 :include-color-wheels? true)
  (global-color-cue "green" 3 0 :include-color-wheels? true)
  (global-color-cue "blue" 4 0 :include-color-wheels? true)
  (global-color-cue "purple" 5 0 :include-color-wheels? true)
  (global-color-cue "white" 6 0 :include-color-wheels? true)

  (ct/set-cue! (:cue-grid *show*) 0 1
               (cues/cue :color (fn [_] (global-color-effect
                                         (params/build-color-param :s :rainbow-saturation :l 50 :h hue-bar)))
                         :short-name "Rainbow Bar Fade"
                         :variables [{:key :rainbow-saturation :name "Saturatn" :min 0 :max 100 :start 100
                                      :type :integer}]))
  (ct/set-cue! (:cue-grid *show*) 1 1
               (cues/cue :color (fn [_] (global-color-effect
                                         (params/build-color-param :s :rainbow-saturation :l 50 :h hue-gradient)
                                         :include-color-wheels? true))
                         :short-name "Rainbow Grid"
                         :variables [{:key :rainbow-saturation :name "Saturatn" :min 0 :max 100 :start 100
                                      :type :integer}]))

  ))

(make-cues)

(fiat-lux)