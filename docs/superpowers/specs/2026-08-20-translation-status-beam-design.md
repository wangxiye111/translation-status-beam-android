# Translation Status Beam Design

## Goal

Create a reusable native Android XML component that renders a 174×64 physical-pixel white translation status capsule with the BorderBeam rotate/md(Large)/ocean/100% moving edge effect.

## Context

The supplied BorderBeam source is the exact visual reference: rotate family, website `Large` (source `size="md"`), `colorVariant="ocean"`, and 100% strength. The target changes the component size to 174×64 px, uses a white HMI surface, and labels it `翻译中`; it keeps the source color variation, inner/stroke/bloom layer order, and animation timing.

The workspace does not contain an Android project, so the deliverable is a portable Kotlin custom View, XML attributes, an example XML layout, and an integration README. The component must not require React Native, WebView, or a web runtime.

## Visual specification

- Reference size: 174×64 px.
- Reference radius: 32 px, producing a capsule.
- Base fill: solid white capsule `#FFFFFF`, with a subtle dark hairline.
- Text: supplied by a normal child `TextView`, centered above the effect layer, using dark `#1D1D1D` for contrast.
- Animated beam: a blue/purple highlight travelling around the outer rounded border, using the source `md(Large) + ocean` palette and rotate mask.
- Glow: visible blue/purple internal diffusion plus bloom, including the source Ocean color variation.
- The inner diffusion shares the same rotating beam window as the border and
  uses only a 2 px white-mode edge feather, so it remains visually connected
  to the stroke.
- The beam must not paint a strong highlight through the text area.
- Default cycle: 1.96 seconds, linear, repeatable, matching the source rotate default.
- Default effect strength: 1.0, matching the requested source configuration.
- Default core stroke: 1 px.
- Default inner shadow blur: 9 px, matching the source md rotate preset.
- Default bloom blur: 8 px, matching the source rotate preset.

## Architecture

Use a `FrameLayout` wrapper in XML. `TranslationStatusView` draws the capsule background and animated effect. A sibling `TextView` owns text, font, accessibility, and localization. The custom View exposes `active`, `strength`, `durationMs`, and `beamEnabled` properties so product state can control the animation without rebuilding the layout.

The renderer uses Android Canvas primitives while preserving the source algorithm: the source Large/md Ocean radial-gradient blob stack is drawn into an offscreen layer, a rotating beam conic mask is applied to that layer, and the inner, stroke, and bloom layers are composited in the same order as the source. A rounded-rectangle ring path replaces the web/Skia SDF mask. The View uses its measured width and height for all geometry; 174×64 is the reference size, not a hard-coded drawing surface.

For broad embedded-Android compatibility, the baseline renderer avoids requiring API 33 `RuntimeShader`. A later API-specific shader path may be added only if device profiling shows the Canvas version cannot meet the target frame rate.

## State and lifecycle

- `active = true`: run the repeating animator.
- `active = false`: animate alpha toward zero, then stop invalidation.
- `strength` is clamped to 0..1 and scales beam alpha, not the base fill.
- Start/resume only while attached and visible; cancel the animator on detach.
- Reduce or disable animation when the host provides a reduced-motion or HMI power-saving state.

## XML contract

The XML wrapper supplies the physical size and clips the rounded capsule. The custom View should accept these attributes:

- `beamActive` boolean, default true.
- `beamStrength` float, default 1.0.
- `beamDurationMs` integer, default 1960.
- `beamEnabled` boolean, default true.
- `beamVariant` string, default `ocean`; accepted values for this component are `ocean` and `mono`.

The child `TextView` remains outside the custom View’s drawing responsibilities.

## Verification

- Render at exactly 174×64 px and confirm the capsule radius is 32 px.
- Capture at least one full animation cycle and confirm there is one continuous travelling highlight.
- Confirm the text remains readable at minimum and maximum strength.
- Confirm `active=false` stops visible motion and releases the animator after fade-out.
- Confirm detach/reattach does not leak an animator or continue invalidation.
- Verify on the actual head-unit API level and GPU; the Canvas fallback is the compatibility baseline.
