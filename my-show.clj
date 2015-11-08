(ns my-show
  "Set up the fixtures, effects, and cues I actually want to use."
  ;; TODO: Your list of required namespaces will differ from this, depending on
  ;;       what fixtures you actually use, and what effects and cues you create.
  (:require [afterglow.core :as core]
            [afterglow.transform :as tf]
            [afterglow.effects.color :refer [color-effect]]
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
            [com.evocomputing.colors :refer [create-color hue adjust-hue]]
            [taoensso.timbre :as timbre]))

(defonce ^{:doc "Holds my show if it has been created,
  so it can be unregistered if it is being re-created."}
  my-show
  (atom nil))

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

  (show/patch-fixture! :hyp-rgb (adj/hypnotic-rgb) universe 1)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

(use-my-show)  ; Set up my show as the default show, using the function above.

;; TODO: Add your custom effects, then assign them to cues with sensible colors
;;       See afterglow.examples for examples.