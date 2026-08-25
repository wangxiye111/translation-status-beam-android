# Translation Status Beam for Native Android

A portable native Android XML/Canvas implementation of the 174×64 px
translation-status Border Beam used by the HMI.

The renderer follows the upstream `rotate + md/Large + ocean` motion model.
The stroke and inner fill use the same original Ocean color palette and the
same hue phase. The current HMI tuning is:

- motion cycle: 1960 ms
- color cycle: 2000 ms
- hue range: 50°
- stroke width: 4 px
- stroke/Bloom saturation: 8
- inner-fill saturation: 12
- white solid capsule surface with dark foreground text

See [android/README-translation-status-beam.md](android/README-translation-status-beam.md)
for XML usage, runtime controls, and head-unit integration notes.

The `com.example.hmi` package is a placeholder. Replace it with the host
vehicle project's package name during integration.
