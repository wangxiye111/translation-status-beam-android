# Translation Status Beam

This folder contains a portable native Android XML implementation for the
174×64 px white translation capsule.

The effect is based on the upstream BorderBeam configuration:

```text
family: rotate
size: md (shown as Large in the website)
colorVariant: ocean
strength: 1.0
duration: 1960ms
```

The source Large/md Ocean palette and beam mask are recorded in
`src/main/kotlin/com/example/hmi/LargeOceanBeamSpec.kt`. The renderer keeps the
source order: inner layer → stroke ring → bloom layer, with a solid white
surface and dark foreground for the HMI presentation. It uses Canvas bitmap
layers instead of React Native or WebView.

For the white HMI adaptation, the source 28 px inner-edge mask is replaced by
a 2 px feather. This keeps the moving inner gradient visually attached to the
moving border instead of creating a white gap between them.

## XML usage

Include `src/main/res/layout/view_translation_status.xml`, or copy the same
structure into the host layout:

```xml
<FrameLayout
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="174px"
    android:layout_height="64px">

    <com.example.hmi.TranslationStatusView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:beamActive="true"
        app:beamEnabled="true"
        app:beamStrength="1.0"
        app:beamDurationMs="1960"
        app:beamVariant="ocean" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:text="翻译中"
        android:textColor="#1D1D1D"
        android:textSize="18px" />
</FrameLayout>
```

The package name `com.example.hmi` is a placeholder. Replace it with the
vehicle project’s package name before integration.

## Runtime controls

```kotlin
translationStatusView.active = isTranslating
translationStatusView.strength = 1.0f
translationStatusView.durationMs = 1960L
translationStatusView.beamEnabled = true
```

The View stops its angle animator when detached or not visible. `strength` is
clamped to 0..1 and affects only the beam layers, not the dark base or
the label.

## Verification on the head unit

1. Confirm the host project’s API level and display density.
2. Render the component at exactly 174×64 physical px.
3. Compare one full 1960 ms cycle against the source `rotate + md(Large) + ocean`
   reference.
4. Check text readability at strength 1.0 and at the product’s reduced
   strength value. The capsule uses the approved white HMI presentation while
   the beam geometry, timing, and Ocean color variation follow the source; the
   only product-specific geometry change is 174×64 px.
5. Profile GPU/CPU time with the real head-unit renderer; this implementation
   deliberately uses a software layer for the tiny blurred effect to support
   older embedded Android versions.
