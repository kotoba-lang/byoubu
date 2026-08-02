# byoubu

**A catalog of backdrops as data — not a folder of videos.**

屏風 (byōbu): the folding screen you stand behind people, historically painted
with landscapes. This library is the same object for a page: the scenery that
sits behind content, and the facts about what that scenery does to the content
on top of it.

```clojure
(require '[byoubu.core :as byoubu])

(byoubu/ids)
;; => [:cobalt-dune :ember-mesa :purple-desert :salt-flat]

(byoubu/facts :purple-desert)
;; => #:byoubu.facts{:content-color "#151124"
;;                   :luminance     0.0069
;;                   :appearance    :dark
;;                   :ink           "#f4f2fa"
;;                   :contrast      16.64
;;                   :accent        "#a887cf"
;;                   :accent-hue    267.5
;;                   :glass-surface :thin}
```

Mounting one in a page is [`kotoba-lang/byoubu-ui`](https://github.com/kotoba-lang/byoubu-ui).

## Why this is not a video library

Commercial backdrop sets ship a looped 4K mp4 and a poster. Two things follow
from that format, and both are why this one is shaped differently.

**A video cannot be re-derived.** You can crop it. You cannot re-render it at
another aspect ratio, shift the time of day, recolor it to a brand, or fork a
sibling variant. Here a backdrop is a `:byoubu/scene` — a spec written in the
vocabulary the existing kami stack already speaks (`sky`, `atmosphere`,
`terrain` biome, `camera`, `postfx` grade) plus a `:byoubu/seed`. Everything a
renderer needs is in the entry, so the picture is reproducible rather than
merely distributable.

**A video says nothing about what may be put on top of it.** Every consumer
re-guesses the text color, the accent, and how opaque the panels need to be,
and at least one of them gets it wrong. `byoubu.facts` computes those answers
from the palette — ink, appearance, accent, WCAG contrast, and which glass
surface tier content wants — deterministically, with no renderer and no
browser. `byoubu.spec` then treats the answer as *structural*: a backdrop
whose own recommended ink fails AA body contrast on its own content band is
rejected by the same validator that rejects a missing palette key. The catalog
cannot contain a backdrop that is unreadable by its own recommendation.

## Three tiers

A backdrop is defined once and delivered at whichever tier the client can take.

| tier | what it is | cost | who renders it |
|---|---|---|---|
| **T0** | `byoubu.plate` — layered CSS gradients derived from the palette | ~1 KB, first frame, no network | this library |
| **T1** | a rendered still | one image | the `:render` alias |
| **T2** | the live WebGPU scene | GPU | the kami stack |

T0 is deliberately not a picture of dunes — CSS gradients cannot draw a ridge
line, and pretending otherwise is how gradient backgrounds come to look cheap.
What it reproduces faithfully is the vertical light structure that governs
legibility: zenith, horizon glow, ground fall-off, vignette, with the skyline
placed from the same `:camera :pitch-deg` the renderer uses. Content on T0 and
content on T2 sit on the same luminance, so nothing shifts when a higher tier
loads.

## Zero runtime dependencies, on purpose

`:deps {}`. A page that wants a backdrop and legible text must not inherit
`sky` / `atmosphere` / `terrain` / `postfx` / `webgpu` (and, through terrain and
postfx, `kotoba-lang/compiler`). Those live in the `:render` alias, used only
by the offline exporter — the same split `liquid-glass-ui` keeps for
reagent/re-frame/shadow-css.

## Adding a backdrop

Add one map to `byoubu.catalog/catalog`. The tests do the rest: structure,
palette vocabulary, content-band weights, seed presence, biome membership in
the set `kami-terrain-scene` actually defines, and AA contrast.

IDs are **stable and immutable once published** — consumers pin them. A
changed look is a new id, not an edited entry.

## Tests

```bash
nbb bin/test.cljs      # ClojureScript
clojure -M:test        # JVM
```

Both, always. The plate emitter formats numbers, and number formatting is
exactly where CLJ and CLJS quietly disagree — `(/ (Math/round x) 10.0)` prints
`62.0` on one and `62` on the other. There is a test for that.

## Status

The catalog, the facts layer and T0 are implemented and tested on both
runtimes. **T1/T2 are not implemented**: `catalog/generator` has `:pin nil`
because no artifact has been rendered from these specs yet, and recording a
pin nobody verified would be a lie the next reader could not detect. The
scene specs are authored in the renderer's vocabulary and validated
structurally; they have not been round-tripped through a GPU.

See `docs/adr/0001-byoubu.md`.
