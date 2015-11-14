(ns lights.my-show
  "Set up the fixtures, effects, and cues I actually want to use."
  ;; TODO: Your list of required namespaces will differ from this, depending on
  ;;       what fixtures you actually use, and what effects and cues you create.
  (:require [afterglow.channels :as chan]
            [afterglow.core :as core]
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

(defn my-rgb
  "A simple RGB light"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/color 4 :white)]
   :name "Simple RGB"})

(defn head280
  "Head PHS 280"
  []
  {:channels [(chan/tilt 1)
              (chan/pan 2)
              (chan/functions :shutter 10 0 "shutter-closed" 32 "shutter-open")
              (chan/dimmer 11)]
   :name "Head PHS 280"})

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
                    (show/show :universes [1] :description "My Show"))))

  ;; 2DO: find what "rgb-1" means and where to look for documentation
  (show/patch-fixture! :rgb-1 (my-rgb) 1 1)

  ;; Code below is working now :)
  (show/patch-fixture! :head-1 (head280) 1 220)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

;; Commenting out this next line so that individual functions in this namespace
;; can be tested while debugging them. It can be uncommented once everything
;; is working; until then, try calling it manually as you add new fixture definitions
;; and patch them.
;(use-my-show)  ; Set up my show as the default show, using the function above.

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

;; TODO: Add your custom effects, then assign them to cues with sensible colors
;;       See afterglow.examples for examples.
