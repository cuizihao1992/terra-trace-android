# TerraTrace Android

This is a small Android-native MapLibre app prototype for a frontend-oriented developer.

## Download

Install the latest debug APK from the GitHub release page:

- [TerraTrace Android v0.1.0](https://github.com/cuizihao1992/terra-trace-android/releases/tag/v0.1.0)
- [Direct APK download](https://github.com/cuizihao1992/terra-trace-android/releases/download/v0.1.0/terra-trace-android-debug.apk)

## App Flow

- `HomeActivity`: landing page with a single entry point.
- `MapActivity`: MapLibre map page with a back button, sample vector base map, sample track, clickable points, and popup dialog.

## Frontend Mental Model

| Web | Android Native |
| --- | --- |
| HTML | XML layout |
| CSS | `res/values`, `res/drawable`, XML attributes |
| JavaScript | Kotlin |
| Browser runtime | Android runtime |
| DOM tree | View tree |

## Map Stack

- Map engine: MapLibre Native Android
- Base style: `https://demotiles.maplibre.org/style.json`
- Small business data: GeoJSON source
- Track rendering: LineLayer
- Click popup: `queryRenderedFeatures`
- WMS/WMTS/MVT extension points: `MapActivity.kt`
