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

## Brand colors

The app's real Material 3 color scheme (`core/ui/theme/Color.kt`) is derived from the actual logo
artwork's colors — this is the canonical record of those 5 source values, as given by the project
owner:

| Color | Hex | Role |
| --- | --- | --- |
| Gold | `#ebc971` | Tertiary/celebratory accent — badges, streak flame, logo glow |
| Dark green | `#053827` | Dark-scheme background/surface — matches `ic_launcher_background` exactly |
| Light green | `#7ed957` | Dark-scheme primary, light-scheme primary container tint |
| Green | `#00bf63` | Brand accent — light-scheme `primary` is a deepened variant (`#007A42`) since the raw value fails WCAG AA against white text (~2.4:1); the raw `#00bf63` still appears directly wherever contrast allows |
| White | `#ffffff` | `onPrimary`, light-scheme text-on-brand-color |

Exposed directly as named constants in `Color.kt` — `BrandGold`, `BrandDarkGreen`,
`BrandLightGreen`, `BrandGreen`, `BrandWhite` — for anything that wants the literal brand hex
rather than a theme role (e.g. `QwLogo`'s glow, which animates through all 4 accent tones rather
than sitting on a single theme color). Prefer a theme role (`MaterialTheme.colorScheme.*`) for
ordinary UI; reach for the `Brand*` constants only when the *exact* logo color itself is the
point, not just "something in the green/gold family."

## Grammar category badges (Parts of Speech)

`core/ui/components/GrammarCategoryBadge.kt` provides color-coded badges indicating grammatical classification:

| Part of Speech | Arabic | Container Color | Icon |
|---|---|---|---|
| **Ism** (Noun) | الاسم | Forest Green (`#2E7D32`) | `Icons.Filled.AutoStories` |
| **Fi'l** (Verb) | الفعل | Warm Gold (`#D4AF37`) | `Icons.Filled.FlashOn` |
| **Ḥarf** (Particle) | الحرف | Sky Blue (`#0288D1`) | `Icons.Filled.AutoAwesome` |

Badges are rendered on exercise headers, 5-mode test cards, and dictionary modals to provide continuous visual grounding.

## Verse span & Tashkīl highlighting

Highlighted Quranic vocabulary words within verse contexts must preserve complete diacritical fidelity (**Tashkīl**, **Ḥarakāt**, **Sukūn**, **Tashdīd**, **Tanwīn**).

- **Continuous inline flow**: Highlighted words within verse spans use in-line text background tinting and bold weight rather than surrounding 3D card borders, preventing line-wrapping anomalies and maintaining natural Arabic typographical flow.
- **Font rendering**: Arabic verses are rendered using `LocalQuranFontFamily.current` with proportional line-height (`fontSize = 20.sp`, `lineHeight = 34.sp`) to prevent diacritic clipping.

## Streamlined font selection

The font selection screen renders clean, focused cards containing solely:
1. Font display name (localized)
2. Live Arabic rendering of **Surah Al-Kawthar** using the target typeface.
All extraneous descriptions, publisher bullet points, and script family tags are omitted for visual clarity.

