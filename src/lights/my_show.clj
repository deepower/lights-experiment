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


(defn make-strobe-cue
  "Create a cue which strobes a set of fixtures as long as the cue pad
  is held down, letting the operator adjust the lightness of the
  strobe color by varying the pressure they are applying to the pad on
  controllers which support pressure sensitivity."
  [name fixtures x y]
  (ct/set-cue! (:cue-grid *show*) x y 
     (cues/cue (keyword (str "strobe-" (clojure.string/replace (clojure.string/lower-case name) " " "-")))
         (fn [var-map] (fun/strobe (str "Strobe " name) fixtures
             (:level var-map 50) (:lightness var-map 100)))
         :color :purple
         :held true
         :priority 100
         :variables [{:key "level" :min 0 :max 100 :start 100 :name "Level"}
           {:key "lightness" :min 0 :max 100 :name "Lightness" :velocity true}])))

(defn x-phase
  "Return a value that ranges from zero for the leftmost fixture in a
  show to 1 for the rightmost, for staggering the phase of an
  oscillator in making a can-can chase."
  [head show]
  (let [dimensions @(:dimensions *show*)]
    (/ (- (:x head) (:min-x dimensions)) (- (:max-x dimensions) (:min-x dimensions)))))

(defn- name-torrent-gobo-cue
  "Come up with a summary name for one of the gobo cues we are
  creating that is concise but meaningful on a controller interface."
  [prefix function]
  (let [simplified (clojure.string/replace (name function) #"^gobo-fixed-" "")
        simplified (clojure.string/replace simplified #"^gobo-moving-" "m/")
        spaced (clojure.string/replace simplified "-" " ")]
    (str (clojure.string/upper-case (name prefix)) " " spaced)))

(defn- make-torrent-gobo-cues
  "Create cues for the fixed and moving gobo options, stationary and
  shaking. Takes up half a page on the Push, with the top left at the
  coordinates specified."
  [prefix fixtures top left]
  ;; Make cues for the stationary and shaking versions of all fixed gobos
  (doseq [_ (map-indexed (fn [i v]
                           (let [blue (create-color :blue)
                                 x (if (< i 8) left (+ left 2))
                                 y (if (< i 8) (- top i) (- top i -1))
                                 cue-key (keyword (str (name prefix) "-gobo-fixed"))]
                             (ct/set-cue! (:cue-grid *show*) x y
                                          (cues/function-cue cue-key (keyword v) fixtures :color blue
                                                             :short-name (name-torrent-gobo-cue prefix v)))
                             (let [function (keyword (str (name v) "-shake"))]
                               (ct/set-cue! (:cue-grid *show*) (inc x) y
                                            (cues/function-cue cue-key function fixtures :color blue
                                                               :short-name (name-torrent-gobo-cue prefix function))))))
                         ["gobo-fixed-mortar" "gobo-fixed-4-rings" "gobo-fixed-atom" "gobo-fixed-jacks"
                          "gobo-fixed-saw" "gobo-fixed-sunflower" "gobo-fixed-45-adapter"
                          "gobo-fixed-star" "gobo-fixed-rose-fingerprint"])])
  ;; Make cues for the stationary and shaking versions of all rotating gobos
  (doseq [_ (map-indexed (fn [i v]
                           (let [green (create-color :green)
                                 cue-key (keyword (str (name prefix) "-gobo-moving"))]
                             (ct/set-cue! (:cue-grid *show*) (+ left 2) (- top i)
                                          (cues/function-cue cue-key (keyword v) fixtures :color green
                                                             :short-name (name-torrent-gobo-cue prefix v)))
                             (let [function (keyword (str (name v) "-shake"))]
                               (ct/set-cue! (:cue-grid *show*) (+ left 3) (- top i)
                                            (cues/function-cue cue-key function fixtures :color green
                                                               :short-name (name-torrent-gobo-cue prefix function))))))
                         ["gobo-moving-rings" "gobo-moving-color-swirl" "gobo-moving-stars"
                          "gobo-moving-optical-tube" "gobo-moving-magenta-bundt"
                          "gobo-moving-blue-mega-hazard" "gobo-moving-turbine"])]))

(defonce ^{:doc "A step parameter for controlling example chase cues.
  Change it to experiment with other kinds of timing and fades."}
  step-param
  (atom nil))

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
    (ct/set-cue! (:cue-grid *show*) 2 1
                 (cues/cue :color (fn [_] (global-color-effect
                                           (params/build-color-param :s :rainbow-saturation :l 50 :h hue-gradient
                                                                     :adjust-hue hue-bar)))
                           :short-name "Rainbow Grid+Bar"
                           :variables [{:key :rainbow-saturation :name "Saturatn" :min 0 :max 100 :start 100
                                        :type :integer}]))
    (ct/set-cue! (:cue-grid *show*) 3 1  ; Desaturate the rainbow as each beat progresses
                 (cues/cue :color (fn [_] (global-color-effect
                                           (params/build-color-param :s desat-beat :l 50 :h hue-gradient
                                                                     :adjust-hue hue-bar)))
                           :short-name "Rainbow Pulse"))

    (ct/set-cue! (:cue-grid *show*) 5 1
                 (cues/cue :color (fn [_] (global-color-effect
                                           (params/build-color-param :s 100 :l 50 :h hue-z-gradient)
                                           :include-color-wheels? true))
                           :short-name "Z Rainbow Grid"))
    (ct/set-cue! (:cue-grid *show*) 6 1
                 (cues/cue :color (fn [_] (global-color-effect
                                           (params/build-color-param :s 100 :l 50 :h hue-z-gradient
                                                                     :adjust-hue hue-bar)))
                           :short-name "Z Rainbow Grid+Bar"))

    (ct/set-cue! (:cue-grid *show*) 7 1
                 (cues/cue :color (fn [_] (global-color-effect
                                           (params/build-color-param :s 100 :l 50 :h hue-gradient
                                                                     :adjust-hue hue-bar)
                                           :lights (show/fixtures-named "blade")))
                           :short-name "Rainbow Blades"))


    ;; TODO: Write a macro to make it easier to bind cue variables.
    (ct/set-cue! (:cue-grid *show*) 0 7
                 (cues/cue :sparkle (fn [var-map] (fun/sparkle (show/all-fixtures)
                                                               :chance (:chance var-map 0.05)
                                                               :fade-time (:fade-time var-map 50)))
                           :held true
                           :priority 100
                           :variables [{:key "chance" :min 0.0 :max 0.4 :start 0.05 :velocity true}
                                       {:key "fade-time" :name "Fade" :min 1 :max 2000 :start 50 :type :integer}]))

    (ct/set-cue! (:cue-grid *show*) 2 7
                 (cues/cue :transform-colors (fn [_] (color-fx/transform-colors (show/all-fixtures)))
                           :priority 1000))


    (ct/set-cue! (:cue-grid *show*) 7 7
                 (cues/function-cue :strobe-all :strobe (show/all-fixtures) :effect-name "Raw Strobe"))


    ;; Dimmer cues to turn on and set brightness of groups of lights
    (ct/set-cue! (:cue-grid *show*) 0 2
                 (cues/cue :dimmers (fn [var-map] (global-dimmer-effect
                                                   (params/bind-keyword-param (:level var-map 255) Number 255)
                                                   :effect-name "All Dimmers"))
                           :variables [{:key "level" :min 0 :max 255 :start 255 :name "Level"}]
                           :color :yellow :end-keys [:torrent-dimmers :blade-dimmers :ws-dimmers
                                                     :puck-dimmers :hex-dimmers :snowball-dimmers]))
    (ct/set-cue! (:cue-grid *show*) 1 2
                 (cues/cue :torrent-dimmers (fn [var-map] (dimmer-effect
                                                           (params/bind-keyword-param (:level var-map 255) Number 255)
                                                           (show/fixtures-named "torrent")
                                                           :effect-name "Torrent Dimmers"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 2 2
                 (cues/cue :blade-dimmers (fn [var-map] (dimmer-effect
                                                         (params/bind-keyword-param (:level var-map 255) Number 255)
                                                         (show/fixtures-named "blade")
                                                         :effect-name "Blade Dimmers"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 3 2
                 (cues/cue :ws-dimmers (fn [var-map] (dimmer-effect
                                                      (params/bind-keyword-param (:level var-map 255) Number 255)
                                                      (show/fixtures-named "ws")
                                                      :effect-name "Weather System Dimmers"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers]))

    (ct/set-cue! (:cue-grid *show*) 4 2
                 (cues/cue :hex-dimmers (fn [var-map] (dimmer-effect
                                                       (params/bind-keyword-param (:level var-map 255) Number 255)
                                                       (show/fixtures-named "hex")
                                                       :effect-name "Hex Dimmers"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 5 2
                 (cues/cue :puck-dimmers (fn [var-map] (dimmer-effect
                                                        (params/bind-keyword-param (:level var-map 255) Number 255)
                                                        (show/fixtures-named "puck")
                                                        :effect-name "Puck Dimmers"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 6 2
                 (cues/cue :snowball-dimmers (fn [var-map] (dimmer-effect
                                                            (params/bind-keyword-param (:level var-map 255) Number 255)
                                                            (show/fixtures-named "snowball")
                                                            :effect-name "Snowball Dimmers"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 7 2
                 (cues/cue :torrent-1-dimmer (fn [var-map] (dimmer-effect
                                                            (params/bind-keyword-param (:level var-map 255) Number 255)
                                                            (show/fixtures-named "torrent-1")
                                                            :effect-name "Torrent 1 Dimmer"))
                           :variables [(merge {:key "level" :min 0 :max 255 :start 255 :name "Level"})]
                           :color :orange :end-keys [:dimmers :torrent-dimmers]))

    ;; Dimmer oscillator cues: Sawtooth down each beat
    (ct/set-cue! (:cue-grid *show*) 0 3
                 (cues/cue :dimmers (fn [_] (global-dimmer-effect
                                             (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                             :effect-name "All Saw Down Beat"))
                           :color :yellow :end-keys [:torrent-dimmers :blade-dimmers :ws-dimmers
                                                     :puck-dimmers :hex-dimmers :snowball-dimmers]))
    (ct/set-cue! (:cue-grid *show*) 1 3
                 (cues/cue :torrent-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                    (show/fixtures-named "torrent") :effect-name "Torrent Saw Down Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 2 3
                 (cues/cue :blade-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                    (show/fixtures-named "blade") :effect-name "Blade Saw Down Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 3 3
                 (cues/cue :ws-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                    (show/fixtures-named "ws") :effect-name "WS Saw Down Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 4 3
                 (cues/cue :hex-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                    (show/fixtures-named "hex") :effect-name "Hex Saw Down Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 5 3
                 (cues/cue :puck-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                    (show/fixtures-named "puck") :effect-name "Puck Saw Down Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 6 3
                 (cues/cue :snowball-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :down? true))
                                    (show/fixtures-named "snowball") :effect-name "Snowball Saw Down Beat"))
                           :color :orange :end-keys [:dimmers]))

    ;; Dimmer oscillator cues: Sawtooth up over 2 beat
    (ct/set-cue! (:cue-grid *show*) 0 4
                 (cues/cue :dimmers (fn [_] (global-dimmer-effect
                                             (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                             :effect-name "All Saw Up 2 Beat"))
                           :color :yellow :end-keys [:torrent-dimmers :blade-dimmers :ws-dimmers
                                                     :puck-dimmers :hex-dimmers :snowball-dimmers]))
    (ct/set-cue! (:cue-grid *show*) 1 4
                 (cues/cue :torrent-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                    (show/fixtures-named "torrent") :effect-name "Torrent Saw Up 2 Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 2 4
                 (cues/cue :blade-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                    (show/fixtures-named "blade") :effect-name "Blade Saw Up 2 Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 3 4
                 (cues/cue :ws-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                    (show/fixtures-named "ws") :effect-name "WS Saw Up 2 Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 4 4
                 (cues/cue :hex-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                    (show/fixtures-named "hex") :effect-name "Hex Saw Up 2 Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 5 4
                 (cues/cue :puck-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                    (show/fixtures-named "puck") :effect-name "Puck Saw Up 2 Beat"))
                           :color :orange :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 6 4
                 (cues/cue :snowball-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sawtooth-beat :beat-ratio 2))
                                    (show/fixtures-named "snowball") :effect-name "Snowball Saw Up 2 Beat"))
                           :color :orange :end-keys [:dimmers]))

    ;; Dimmer oscillator cues: Sine over a bar
    (ct/set-cue! (:cue-grid *show*) 0 5
                 (cues/cue :dimmers (fn [_] (global-dimmer-effect
                                             (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                             :effect-name "All Sine Bar"))
                           :color :cyan :end-keys [:torrent-dimmers :blade-dimmers :ws-dimmers
                                                   :puck-dimmers :hex-dimmers :snowball-dimmers]))
    (ct/set-cue! (:cue-grid *show*) 1 5
                 (cues/cue :torrent-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                    (show/fixtures-named "torrent") :effect-name "Torrent Sine Bar"))
                           :color :blue :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 2 5
                 (cues/cue :blade-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                    (show/fixtures-named "blade") :effect-name "Blade Sine Bar"))
                           :color :blue :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 3 5
                 (cues/cue :ws-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                    (show/fixtures-named "ws") :effect-name "WS Sine Bar"))
                           :color :blue :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 4 5
                 (cues/cue :hex-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                    (show/fixtures-named "hex") :effect-name "Hex Sine Bar"))
                           :color :blue :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 5 5
                 (cues/cue :puck-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                    (show/fixtures-named "puck") :effect-name "Puck Sine Bar"))
                           :color :blue :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 6 5
                 (cues/cue :snowball-dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/sine-bar) :min 1)
                                    (show/fixtures-named "snowball") :effect-name "Snowball Sine Bar"))
                           :color :blue :end-keys [:dimmers]))
    (ct/set-cue! (:cue-grid *show*) 7 5
                 (cues/cue :dimmers
                           (fn [_] (dimmer-effect
                                    (params/build-oscillated-param (oscillators/triangle-bar) :min 1)
                                    (show/all-fixtures) :effect-name "All Triangle Bar"))
                           :color :red :end-keys [:torrent-dimmers :blade-dimmers :ws-dimmers
                                                  :puck-dimmers :hex-dimmers :snowball-dimmers]))

    ;; Strobe cues
    (make-strobe-cue "All" (show/all-fixtures) 0 6)
    (make-strobe-cue "Torrents" (show/fixtures-named "torrent") 1 6)
    (make-strobe-cue "Blades" (show/fixtures-named "blade") 2 6)
    (make-strobe-cue "Weather Systems" (show/fixtures-named "ws") 3 6)
    (make-strobe-cue "Hexes" (show/fixtures-named "hex") 4 6)
    (make-strobe-cue "Pucks" (show/fixtures-named "puck") 5 6)

    (ct/set-cue! (:cue-grid *show*) 7 6
                 (cues/cue :adjust-strobe (fn [_] (fun/adjust-strobe))
                           :color :purple
                           :variables [{:key :strobe-hue :min 0 :max 360 :name "Hue" :centered true}
                                       {:key :strobe-saturation :min 0 :max 100 :name "Saturatn"}]))

    ;; The upper page of torrent config cues
    (ct/set-cue! (:cue-grid *show*) 0 15
                 (cues/function-cue :torrent-shutter :shutter-open (show/fixtures-named "torrent")))
    (ct/set-cue! (:cue-grid *show*) 1 15
                 (cues/function-cue :torrent-reset :motor-reset (show/fixtures-named "torrent")
                                    :color (create-color :red) :held true))

    (ct/set-cue! (:cue-grid *show*) 6 15
                 (cues/function-cue :t1-focus :focus (show/fixtures-named "torrent-1") :effect-name "Torrent 1 Focus"
                                    :color (create-color :yellow)))
    (ct/set-cue! (:cue-grid *show*) 7 15
                 (cues/function-cue :t2-focus :focus (show/fixtures-named "torrent-2") :effect-name "Torrent 2 Focus"
                                    :color (create-color :yellow)))
    (ct/set-cue! (:cue-grid *show*) 6 14
                 (cues/function-cue :t1-prism :prism-clockwise (show/fixtures-named "torrent-1") :level 100
                                    :effect-name "T1 Prism Spin CW" :color (create-color :orange)))
    (ct/set-cue! (:cue-grid *show*) 7 14
                 (cues/function-cue :t2-prism :prism-clockwise (show/fixtures-named "torrent-2") :level 100
                                    :effect-name "T2 Prism Spin CW" :color (create-color :orange)))
    (ct/set-cue! (:cue-grid *show*) 6 13
                 (cues/function-cue :t1-prism :prism-in (show/fixtures-named "torrent-1")
                                    :effect-name "T1 Prism In" :color (create-color :orange)))
    (ct/set-cue! (:cue-grid *show*) 7 13
                 (cues/function-cue :t2-prism :prism-in (show/fixtures-named "torrent-2")
                                    :effect-name "T2 Prism In" :color (create-color :orange)))
    (ct/set-cue! (:cue-grid *show*) 6 12
                 (cues/function-cue :t1-prism :prism-counterclockwise (show/fixtures-named "torrent-1")
                                    :effect-name "T1 Prism Spin CCW" :color (create-color :orange)))
    (ct/set-cue! (:cue-grid *show*) 7 12
                 (cues/function-cue :t2-prism :prism-counterclockwise (show/fixtures-named "torrent-2")
                                    :effect-name "T2 Prism Spin CCW" :color (create-color :orange)))
    (ct/set-cue! (:cue-grid *show*) 6 11
                 (cues/function-cue :t1-gobo-fixed :gobo-fixed-clockwise (show/fixtures-named "torrent-1")
                                    :effect-name "T1 Fixed Gobos Swap CW" :color (create-color :blue)))
    (ct/set-cue! (:cue-grid *show*) 7 11
                 (cues/function-cue :t2-gobo-fixed :gobo-fixed-clockwise (show/fixtures-named "torrent-2")
                                    :effect-name "T2 Fixed Gobos Swap CW" :color (create-color :blue)))
    (ct/set-cue! (:cue-grid *show*) 6 10
                 (cues/function-cue :t1-gobo-moving :gobo-moving-clockwise (show/fixtures-named "torrent-1")
                                    :effect-name "T1 Moving Gobos Swap CW" :color (create-color :green)))
    (ct/set-cue! (:cue-grid *show*) 7 10
                 (cues/function-cue :t2-gobo-moving :gobo-moving-clockwise (show/fixtures-named "torrent-2")
                                    :effect-name "T2 Moving Gobos Swap CW" :color (create-color :green)))
    (ct/set-cue! (:cue-grid *show*) 6 9
                 (cues/function-cue :t1-gobo-rotation :gobo-rotation-clockwise (show/fixtures-named "torrent-1")
                                    :effect-name "T1 Spin Gobo CW" :color (create-color :cyan) :level 100))
    (ct/set-cue! (:cue-grid *show*) 7 9
                 (cues/function-cue :t2-gobo-rotation :gobo-rotation-clockwise (show/fixtures-named "torrent-2")
                                    :effect-name "T2 Spin Gobo CW" :color (create-color :cyan) :level 100))
    (ct/set-cue! (:cue-grid *show*) 6 8
                 (cues/function-cue :t1-gobo-rotation :gobo-rotation-counterclockwise (show/fixtures-named "torrent-1")
                                    :effect-name "T1 Spin Gobo CCW" :color (create-color :cyan)))
    (ct/set-cue! (:cue-grid *show*) 7 8
                 (cues/function-cue :t2-gobo-rotation :gobo-rotation-counterclockwise (show/fixtures-named "torrent-2")
                                    :effect-name "T2 Spin Gobo CCW" :color (create-color :cyan)))

    ;; Some basic moving head chases
    (let [triangle-phrase (params/build-oscillated-param ; Move back and forth over a phrase
                           (oscillators/triangle-phrase) :min -90 :max 90)
          staggered-triangle-bar (params/build-spatial-param ; Bounce over a bar, staggered across grid x
                                  (show/all-fixtures)
                                  (fn [head]
                                    (params/build-oscillated-param
                                     (oscillators/triangle-bar :phase (x-phase head *show*))
                                     :min -90 :max 0)))
          can-can-dir (params/build-direction-param-from-pan-tilt :pan triangle-phrase :tilt staggered-triangle-bar)
          can-can-p-t (params/build-pan-tilt-param :pan triangle-phrase :tilt staggered-triangle-bar)]
      (ct/set-cue! (:cue-grid *show*) 0 9
                   (cues/cue :movement (fn [var-map]
                                         (move/direction-effect "Can Can" can-can-dir (show/all-fixtures)))))
      (ct/set-cue! (:cue-grid *show*) 1 9
                   (cues/cue :movement (fn [var-map]
                                         (move/pan-tilt-effect "P/T Can Can" can-can-p-t (show/all-fixtures))))))
    
    ;; A couple snowball cues
    (ct/set-cue! (:cue-grid *show*) 0 10 (cues/function-cue :sb-pos :beams-fixed (show/fixtures-named "snowball")
                                                            :effect-name "Snowball Fixed"))
    (ct/set-cue! (:cue-grid *show*) 1 10 (cues/function-cue :sb-pos :beams-moving (show/fixtures-named "snowball")
                                                            :effect-name "Snowball Moving"))

    ;; The separate page of specific gobo cues for each Torrent
    (make-torrent-gobo-cues :t1 (show/fixtures-named "torrent-1") 15 8)
    (make-torrent-gobo-cues :t2 (show/fixtures-named "torrent-2") 15 12)

    ;; TODO: Write a function to create direction cues, like function cues? Unless macro solves.
    (ct/set-cue! (:cue-grid *show*) 0 8
                 (cues/cue :torrent-dir (fn [var-map]
                                          (move/direction-effect
                                           "Pan/Tilt"
                                           (params/build-direction-param-from-pan-tilt :pan (:pan var-map 0.0)
                                                                                       :tilt (:tilt var-map 0.0)
                                                                                       :degrees true)
                                           (show/all-fixtures)))
                           :variables [{:key "pan" :name "Pan"
                                        :min -180.0 :max 180.0 :start 0.0 :centered true :resolution 0.5}
                                       {:key "tilt" :name "Tilt"
                                        :min -180.0 :max 180.0 :start 0.0 :centered true :resolution 0.5}]))
    (ct/set-cue! (:cue-grid *show*) 1 8
                 (cues/cue :torrent-dir (fn [var-map]
                                          (move/aim-effect
                                           "Aim"
                                           (params/build-aim-param :x (:x var-map 0.0)
                                                                   :y (:y var-map 0.0)
                                                                   :z (:z var-map 1.0))
                                           (show/all-fixtures)))
                           :variables [{:key "x" :name "X"
                                        :min -20.0 :max 20.0 :start 0.0 :centered true :resolution 0.05}
                                       {:key "z" :name "Z"
                                        :min -20.0 :max 20.0 :start 0.0 :centered true :resolution 0.05}
                                       {:key "y" :name "Y"
                                        :min 0.0 :max 20.0 :start 0.0 :centered false :resolution 0.05}]))
    (ct/set-cue! (:cue-grid *show*) 3 8
                 (cues/function-cue :blade-speed :movement-speed (show/fixtures-named "blade")
                                    :color :purple :effect-name "Slow Blades"))

    ;; Some fades
    (ct/set-cue! (:cue-grid *show*) 0 12
                 (cues/cue :color-fade (fn [var-map]
                                         (fx/fade "Color Fade"
                                                  (global-color-effect :red :include-color-wheels? true)
                                                  (global-color-effect :green :include-color-wheels? true)
                                                  (params/bind-keyword-param (:phase var-map 0) Number 0)))
                           :variables [{:key "phase" :min 0.0 :max 1.0 :start 0.0 :name "Fade"}]
                           :color :yellow))

    (ct/set-cue!
     (:cue-grid *show*) 1 12
     (cues/cue :fade-test (fn [var-map]
                            (fx/fade "Fade Test"
                                     (fx/blank)
                                     (global-color-effect :blue :include-color-wheels? true)
                                     (params/bind-keyword-param (:phase var-map 0) Number 0)))
               :variables [{:key "phase" :min 0.0 :max 1.0 :start 0.0 :name "Fade"}]
               :color :cyan))

    (ct/set-cue!
     (:cue-grid *show*) 2 12
     (cues/cue :fade-test-2 (fn [var-map]
                              (fx/fade "Fade Test 2"
                                       (move/direction-effect
                                        "p/t" (params/build-direction-param-from-pan-tilt :pan 0 :tilt 0)
                                        (show/fixtures-named "torrent"))
                                       (move/direction-effect
                                        "p/t" (params/build-direction-param-from-pan-tilt :pan 0 :tilt 0)
                                        (show/fixtures-named "blade"))
                                       (params/bind-keyword-param (:phase var-map 0) Number 0)))
               :variables [{:key "phase" :min 0.0 :max 1.0 :start 0.0 :name "Fade"}]
               :color :red))

    (ct/set-cue!
     (:cue-grid *show*) 3 12
     (cues/cue :fade-test-3 (fn [var-map]
                              (fx/fade "Fade Test P/T"
                                       (move/pan-tilt-effect
                                        "p/t" (params/build-pan-tilt-param :pan 0 :tilt 0)
                                        (show/fixtures-named "torrent"))
                                       (move/pan-tilt-effect
                                        "p/t" (params/build-pan-tilt-param :pan 0 :tilt 0)
                                        (show/fixtures-named "blade"))
                                       (params/bind-keyword-param (:phase var-map 0) Number 0)))
               :variables [{:key "phase" :min 0.0 :max 1.0 :start 0.0 :name "Fade"}]
               :color :orange))

    ;; Some chases
    (ct/set-cue!
     (:cue-grid *show*) 0 13
     (cues/cue :chase (fn [var-map]
                        (fx/chase "Chase Test"
                                  [(global-color-effect :red :include-color-wheels? true)
                                   (global-color-effect :green :lights (show/fixtures-named "hex"))
                                   (global-color-effect :blue :include-color-wheels? true)]
                                  (params/bind-keyword-param (:position var-map 0) Number 0)
                                  :beyond :bounce)
                        )
               :variables [{:key "position" :min -0.5 :max 10.5 :start 0.0 :name "Position"}]
               :color :purple))

    ;; Set up an initial value for our step parameter
    (reset! step-param (params/build-step-param :fade-fraction 0.3 :fade-curve :sine))

    (ct/set-cue!
     (:cue-grid *show*) 1 13
     (cues/cue :chase (fn [var-map]
                        (fx/chase "Chase Test 2"
                                  [(global-color-effect :red :lights (show/fixtures-named "hex"))
                                   (global-color-effect :green :lights (show/fixtures-named "blade"))
                                   (global-color-effect :blue :lights (show/fixtures-named "hex"))
                                   (global-color-effect :white :lights (show/all-fixtures))]
                                  @step-param :beyond :loop))
               :color :magenta))

    ;; Some compound cues
    (ct/set-cue! (:cue-grid *show*) 8 0
                 (cues/cue :star-swirl (fn [_] (cues/compound-cues-effect
                                                "Star Swirl" *show* [[8 12]
                                                                     [10 9]
                                                                     [6 15 {:level 60}]
                                                                     [6 8 {:level 25}]]))))
    ;; Some color cycle chases
    (ct/set-cue! (:cue-grid *show*) 8 1
                 (cues/cue :color (fn [_] (fun/iris-out-color-cycle-chase (show/all-fixtures)))))
    (ct/set-cue! (:cue-grid *show*) 9 1
                 (cues/cue :color (fn [_] (fun/wipe-right-color-cycle-chase
                                           (show/all-fixtures) :transition-phase-function rhythm/snapshot-bar-phase))))
    (ct/set-cue! (:cue-grid *show*) 10 1
                 (cues/cue :color (fn [_] (fun/wipe-right-color-cycle-chase
                                           (show/all-fixtures)
                                           :color-index-function rhythm/snapshot-beat-within-phrase
                                           :transition-phase-function rhythm/snapshot-beat-phase
                                           :effect-name "Wipe Right Beat"))))

    ;; Some cues to show the Hypnotic RGB Laser
    (ct/set-cue! (:cue-grid *show*) 8 3
                 (cues/function-cue :hypnotic-beam :beam-red (show/fixtures-named "hyp-rgb")
                                    :color :red :effect-name "Hypnotic Red"))
    (ct/set-cue! (:cue-grid *show*) 9 3
                 (cues/function-cue :hypnotic-beam :beam-green (show/fixtures-named "hyp-rgb")
                                    :color :green :effect-name "Hypnotic Green"))
    (ct/set-cue! (:cue-grid *show*) 10 3
                 (cues/function-cue :hypnotic-beam :beam-blue (show/fixtures-named "hyp-rgb")
                                    :color :blue :effect-name "Hypnotic Blue"))
    (ct/set-cue! (:cue-grid *show*) 11 3
                 (cues/function-cue :hypnotic-beam :beam-red-green (show/fixtures-named "hyp-rgb")
                                    :color :yellow :effect-name "Hypnotic Red Green"))
    (ct/set-cue! (:cue-grid *show*) 12 3
                 (cues/function-cue :hypnotic-beam :beam-red-blue (show/fixtures-named "hyp-rgb")
                                    :color :purple :effect-name "Hypnotic Red Blue"))
    (ct/set-cue! (:cue-grid *show*) 13 3
                 (cues/function-cue :hypnotic-beam :beam-green-blue (show/fixtures-named "hyp-rgb")
                                    :color :cyan :effect-name "Hypnotic Green Blue"))
    (ct/set-cue! (:cue-grid *show*) 14 3
                 (cues/function-cue :hypnotic-beam :beam-red-green-blue (show/fixtures-named "hyp-rgb")
                                    :color :white :effect-name "Hypnotic Red Green Blue"))
    (ct/set-cue! (:cue-grid *show*) 15 3
                 (cues/function-cue :hypnotic-beam :beam-all-random (show/fixtures-named "hyp-rgb")
                                    :color :white :effect-name "Hypnotic Random"))
    (ct/set-cue! (:cue-grid *show*) 14 4
                 (cues/function-cue :hypnotic-spin :beams-ccw (show/fixtures-named "hyp-rgb")
                                    :color :cyan :effect-name "Hypnotic Rotate CCW" :level 50))
    (ct/set-cue! (:cue-grid *show*) 15 4
                 (cues/function-cue :hypnotic-spin :beams-cw (show/fixtures-named "hyp-rgb")
                                    :color :cyan :effect-name "Hypnotic Rotate Clockwise" :level 50))

    ;; What else?
    ;; TODO: Refine this and make a cue
    #_(show/add-effect! :torrent-focus (afterglow.effects.channel/function-effect
                                        "F" :focus (params/build-oscillated-param (oscillators/sine-bar)
                                                                                  :min 20 :max 200)
                                        (show/fixtures-named "torrent")))))


(make-cues)

(fiat-lux)