# UI Guidelines — the "3D box" visual language

QuranicWords aims for a tactile, game-like feel throughout: every interactive surface should read
as a physical object with depth, not a flat Material default. Compose has no true 3D primitive,
so this is built from three consistent techniques rather than one component — use all three
together on hero surfaces (word cards, chapter/section cards, matching tiles), and at least the
first two on every other tappable surface.

## 1. Named elevation, not literal `dp`

`core/ui/theme/Elevation.kt` defines four steps — `flat`, `pressed`, `raised`, `floating`. Pick
from this set instead of writing `4.dp` inline, so depth reads consistently across screens: a
card at rest is always `raised`, the same card mid-press is always `pressed`, and only genuinely
hero/floating elements (flip-cards, celebratory badges) reach `floating`.

## 2. Press-depth motion

`Modifier.pressDepth(interactionSource)` (`core/ui/motion/PressDepth.kt`) is the shared
press-feedback modifier — a slight scale-down plus forward tilt (`graphicsLayer` `rotationX` +
`cameraDistance`) on press, springing back on release. Chain it onto any interactive surface's
modifier, passing the *same* `InteractionSource` used by that surface's `clickable`/`selectable`/
etc. Collapses to a flat, untilted state under reduced motion automatically. Don't duplicate this
logic inline per-component — extend the shared modifier if a new variant is needed.

## 3. Layered fake shadow for hero surfaces

For a genuinely skeuomorphic, "sitting above the page" look on hero elements (flip-cards,
chapter/section cards), layer a second, blurred, darker-tinted `Box` slightly offset behind the
real content instead of relying on Material's flat drop shadow:

```kotlin
Box {
    // Shadow layer: same shape, offset down-right a few dp, blurred, low-alpha dark tint.
    Box(
        Modifier
            .matchParentSize()
            .offset(x = 3.dp, y = 5.dp)
            .blur(10.dp)
            .background(Color.Black.copy(alpha = 0.18f), shape)
    )
    // Real content layer, at Elevation.floating.
    Card(shape = shape, elevation = CardDefaults.cardElevation(Elevation.floating)) { ... }
}
```

Reserve this for a handful of hero surfaces, not every card on screen — overused, it reads as
noise rather than depth.

## Shape tokens

`core/ui/theme/Shape.kt`'s `QwShapes` (extraSmall…extraLarge `RoundedCornerShape`) plus
`MedallionShapeDefault` (scalloped medallion, reserved for streak/celebratory badges) are the
existing shape vocabulary — reuse them rather than inventing new corner radii per screen.

## Motif vocabulary

`core/ui/components/IslamicMotif.kt` is the single dispatcher for this app's custom-drawn (never
externally licensed) illustration language — `MotifKind.CRESCENT` (crescent moon), `STARFIELD`
(scattered 8-point stars), `MOSQUE` (a mosque silhouette), `BOOK` (an abstract closed-book form),
in that priority order. Each is pure `Path`/`Canvas` math, same discipline as
`GeometricPatternBackground`/`StreakFlame` — no bitmap or SVG assets, no external licensing
question to track. **Reuse `IslamicMotif(kind, ...)` for any new decorative motif need — don't
hand-roll a fifth one per screen.**

Current usage: Splash (crescent + starfield, low-alpha, alongside the geometric lattice),
`LessonSummaryScreen` (crescent on a streak increase, mosque silhouette on a passed section/
chapter exam — the biggest everyday milestones get the biggest visual payoff), Settings' About
section (a small book motif near the brand logo), Home (a very low-alpha starfield behind the
points/streak status strip), and the achievements system's badge medallions (`AchievementBadge`,
category→motif: streak→crescent, chapter/section completion→mosque, vocabulary/coverage→book,
general/first-time→starfield).

Both hard constraints apply to every motif here: no human faces or figures (enforced by
construction — none of the four motifs has anywhere to put one), and `AbstractBookMotif` in
particular must never render actual letterforms or Arabic glyphs — it's a spine/page-edge
suggestion, not a title.
