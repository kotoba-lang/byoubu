(ns byoubu.facts
  "Legibility facts derived from a backdrop's palette.

  This is the namespace that makes the catalog worth more than a folder of
  videos. A distributed background asset says nothing about what may be put
  on top of it; every consumer re-guesses the text color, the accent, and
  how opaque the panels need to be, and gets it wrong at least once. Here
  those answers are *computed* from the palette — deterministically, with no
  renderer and no browser — and the computation is testable, so a backdrop
  that would make body text unreadable fails a test instead of shipping.

  Everything is a pure function of the backdrop map, so facts cannot drift
  from the palette they describe: there is no stored copy to forget to
  update."
  (:require [byoubu.color :as color]))

(def ink-candidates
  "The two inks a backdrop may ask content to use. Not pure #fff / #000:
  the light ink is very slightly cool and the dark ink very slightly warm,
  which is what keeps large text off a backdrop from vibrating."
  {:light "#f4f2fa"
   :dark  "#12100f"})

(defn content-color
  "The single color that best represents what a reader sees behind body
  content — the weighted mix named by `:byoubu/content-band`.

  A backdrop is not one color, so any single answer is an approximation.
  The honest version of the approximation is a *declared* weighting that a
  reviewer can argue with, rather than a number sampled from one pixel of a
  poster that nobody can reproduce."
  [backdrop]
  (color/mix (for [[k w] (:byoubu/content-band backdrop)]
               [(get-in backdrop [:byoubu/palette k]) w])))

(defn- best-ink
  "The ink with the higher contrast against `bg`, as [key hex ratio]."
  [bg]
  (->> ink-candidates
       (map (fn [[k hex]] [k hex (or (color/contrast-ratio hex bg) 0.0)]))
       (sort-by (fn [[_ _ r]] (- r)))
       first))

(def ^:private texture->surface
  "How much material content needs between itself and the backdrop. A busier
  backdrop needs a thicker glass surface to stay readable — this is the
  token liquid-glass-ui already understands, not a new vocabulary."
  {:calm     :thin
   :moderate :regular
   :busy     :thick})

(defn derive-facts
  "Backdrop -> the facts a UI needs to place content on it:

    :byoubu.facts/content-color    weighted mix behind body content
    :byoubu.facts/luminance        its WCAG relative luminance, 0.0-1.0
    :byoubu.facts/appearance       :dark | :light — which HIG appearance
                                   content should resolve to
    :byoubu.facts/ink              recommended text color
    :byoubu.facts/contrast         WCAG ratio of that ink on that mix
    :byoubu.facts/accent           the palette's accent hex (the backdrop's
                                   own chromatic note, for :hig/color :tint)
    :byoubu.facts/accent-hue       its hue in degrees, for queries
    :byoubu.facts/glass-surface    :thin | :regular | :thick

  `:appearance` follows the ink, not the luminance threshold directly: the
  question a consumer actually has is \"which appearance keeps my text
  readable\", and that is exactly the ink comparison."
  [backdrop]
  (let [bg (content-color backdrop)
        [ink-key ink ratio] (best-ink bg)
        accent (get-in backdrop [:byoubu/palette (:byoubu/accent backdrop)])]
    {:byoubu.facts/content-color bg
     :byoubu.facts/luminance     (color/relative-luminance bg)
     ;; light ink => the surrounding UI is a dark appearance, and vice versa.
     :byoubu.facts/appearance    (if (= ink-key :light) :dark :light)
     :byoubu.facts/ink           ink
     :byoubu.facts/contrast      ratio
     :byoubu.facts/accent        accent
     :byoubu.facts/accent-hue    (color/hue accent)
     :byoubu.facts/glass-surface (get texture->surface
                                      (:byoubu/texture backdrop) :regular)}))

(def wcag-aa-body
  "WCAG 2.x AA for body-size text. `byoubu.spec/problems` refuses to accept a
  backdrop whose derived ink does not clear this against its own content
  band — the catalog cannot contain a backdrop that is unreadable by its own
  recommendation."
  4.5)

(defn readable?
  "Does the derived ink clear AA body contrast on this backdrop?"
  [backdrop]
  (>= (or (:byoubu.facts/contrast (derive-facts backdrop)) 0.0) wcag-aa-body))
