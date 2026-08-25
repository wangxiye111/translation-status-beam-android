# Translation Status Beam Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a portable native Android XML component for the 174×64 px translation status capsule.

**Architecture:** A Kotlin `TranslationStatusView` draws the source dark capsule and BorderBeam animated edge using Canvas, gradients, rounded-rect geometry, and layered paints. An XML `FrameLayout` overlays a normal `TextView` so text and accessibility remain independent from rendering.

**Tech Stack:** Kotlin, Android Custom View, Canvas, Paint, LinearGradient, SweepGradient, RadialGradient, ValueAnimator, XML layout/resources.

**Spec:** `docs/superpowers/specs/2026-08-20-translation-status-beam-design.md`

## Global Constraints

- Reference size is 174×64 physical px.
- Reference radius is 32 px.
- The component must work without React Native, WebView, or API 33-only RuntimeShader.
- The beam is decorative and must not own text or accessibility content.
- Default duration is 1960 ms and default strength is 1.0, matching the requested `rotate + md(Large) + ocean` source configuration.
- The visual palette and geometry come from the source Large/md Ocean radial-gradient blob data and beam mask, not a generic sweep-gradient replacement.
- The animator must stop when the View is detached or not visible.

---

### Task 1: Create the custom View contract

**Files:**
- Create: `android/src/main/kotlin/com/example/hmi/TranslationStatusView.kt`
- Create: `android/src/main/res/values/attrs.xml`

**Interfaces:**
- Produces `TranslationStatusView(Context, AttributeSet?)`.
- Exposes `var active: Boolean`, `var strength: Float`, `var durationMs: Long`, and `var beamEnabled: Boolean`.
- Exposes `var beamVariant: BeamVariant` with `OCEAN` and `MONO` values.
- Reads XML attributes `beamActive`, `beamStrength`, `beamDurationMs`, `beamEnabled`, and `beamVariant`.

- [x] **Step 1: Define the attribute contract**

Add a `declare-styleable` named `TranslationStatusView` with the five attributes and defaults documented in the spec. Define `beamVariant` as an enum with `ocean` and `mono` entries.

- [x] **Step 2: Add the View state fields**

Define clamped setters for `strength`, positive validation for `durationMs`, and a boolean setter that starts or stops the animation only when the View is attached.

- [x] **Step 3: Run the Android compile check**

Run the host project’s Android compilation command once the component is placed into an Android project. Expected: the resource and Kotlin files compile without unresolved custom attributes.

### Task 2: Implement the static capsule renderer

**Files:**
- Modify: `android/src/main/kotlin/com/example/hmi/TranslationStatusView.kt`

**Interfaces:**
- Consumes the measured View width and height.
- Produces a rounded capsule fill using a `LinearGradient` and a clipped Canvas.

- [x] **Step 1: Add size-derived geometry**

In `onSizeChanged`, calculate the inset, radius, and rounded-rect bounds from the current width and height. Use `radius = height / 2f` for the capsule reference shape, clamped so it cannot exceed half the height.

- [x] **Step 2: Draw the source dark base**

In `onDraw`, draw the source dark capsule base and subtle light hairline. Clip all subsequent effect layers to the same rounded capsule bounds and preserve the source color variation.

- [x] **Step 3: Add a static preview layout**

Create `android/src/main/res/layout/view_translation_status.xml` containing a `FrameLayout` with a `TranslationStatusView` sized at `174px` by `64px`, `app:beamVariant="ocean"`, and a centered `TextView` with the text `翻译中`.

### Task 3: Implement the travelling beam and bloom

**Files:**
- Modify: `android/src/main/kotlin/com/example/hmi/TranslationStatusView.kt`

**Interfaces:**
- Consumes the animator’s `angle` value.
- Produces a crisp edge stroke and a wider low-alpha bloom.

- [x] **Step 1: Create the source Large/md Ocean blob and mask data**

Add the source Large/md Ocean border/inner blobs, the rotate `beamMaskStops`, the dark rotate white/bloom stops, and the source values `duration=1960`, `innerShadowBlur.md=9`, and `bloomBlurPx=8`. Keep these values in a focused spec file so they can be compared directly with the upstream repository.

- [x] **Step 2: Render the blob layers**

Render each source ellipse into an offscreen bitmap using an elliptical radial-gradient approximation. Composite the inner layer, then the ring stroke layer, then the blurred bloom layer. Do not collapse these into one sweep-gradient stroke.

- [x] **Step 3: Apply the rotating Large/md mask**

Build a rotating alpha `SweepGradient` from the source `beamMaskStops` and apply it to the offscreen blob layers with a destination-in mask. Use the source conic orientation and a single 0..360 linear rotation over 1960 ms.

- [x] **Step 4: Draw the bloom layer**

Draw the source bloom/conic layer into an offscreen bitmap and apply an 8 px blur. Use `BlurMaskFilter` only if it renders correctly on the target hardware; otherwise use two narrower translucent strokes as the compatibility fallback.

- [x] **Step 5: Draw the core layer**

Draw a 1 px rounded-rect stroke with higher alpha and no large blur. Multiply both beam-layer alphas by `strength`; never multiply the dark base by `strength`.

### Task 4: Add animation and lifecycle handling

**Files:**
- Modify: `android/src/main/kotlin/com/example/hmi/TranslationStatusView.kt`

**Interfaces:**
- `active=true` runs a 0..360 degree repeating linear animation.
- `active=false` fades beam alpha to zero and cancels the animator.

- [x] **Step 1: Add the repeating animator**

Create a `ValueAnimator.ofFloat(0f, 360f)` with `durationMs`, `LinearInterpolator`, and `RESTART` repeat mode. On each update, store the angle and call `postInvalidateOnAnimation()`.

- [x] **Step 2: Gate animation by lifecycle**

Start only when attached, visible, enabled, and active. Cancel in `onDetachedFromWindow`; restart in `onAttachedToWindow` when active.

- [x] **Step 3: Verify state transitions**

Exercise active → inactive → active and attach → detach → attach. Expected: no stale animator, no continuing invalidation after detach, and a smooth restart.

### Task 5: Add developer handoff and visual verification

**Files:**
- Create: `android/README-translation-status-beam.md`
- Create: `android/src/test/kotlin/com/example/hmi/TranslationBeamStateTest.kt`

**Interfaces:**
- README documents XML usage, pixel sizing, runtime attributes, and device verification.
- Tests verify state clamping and animator lifecycle decisions that do not require a GPU.

- [x] **Step 1: Document XML usage**

Show the `FrameLayout` wrapper, the custom View attributes, and the centered `TextView`. State clearly that `174px × 64px` is a physical-pixel reference for the fixed HMI target.

- [x] **Step 2: Add state tests**

Test that strength values below 0 become 0, values above 1 become 1, non-positive duration is replaced with 1960 ms, and inactive state prevents the animator from running.

- [ ] **Step 3: Run verification**

Run the host project’s unit tests and Android lint/compile tasks. Capture the component at 174×64 px for one full 1960 ms cycle on the target head unit. Expected: readable `翻译中`, the source Large/md Ocean travelling edge highlight with visible internal diffusion, no clipping outside the capsule, and no visible frame drops.
