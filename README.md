# TerraTrace Android

This is a small Android-native MapLibre app prototype for a frontend-oriented developer.

## Download

Install the latest debug APK from the GitHub release page:

- [TerraTrace Android v0.3.0](https://github.com/cuizihao1992/terra-trace-android/releases/tag/v0.3.0)
- [Direct APK download](https://github.com/cuizihao1992/terra-trace-android/releases/download/v0.3.0/terra-trace-android-demo-v0.3.0-debug.apk)

## Modules

- `app`: sample Android app and APK demo.
- `terra-trace-map`: reusable Android map SDK module built on MapLibre Native.

## App Flow

- `HomeActivity`: landing page with a single entry point.
- `MapActivity`: sample host page that embeds `TerraTraceMapView`.

## Demo Capabilities

- Vector basemap through MapLibre style JSON.
- GeoJSON track rendered as a line layer.
- GeoJSON points rendered as a circle layer.
- GeoJSON polygon rendered as a fill layer.
- Feature click callback and native popup.
- Layer toggles for track, points, polygon, WMS, WMTS, and MVT.
- Location button using the host app's Android location permission flow.
- Zoom in/out buttons.
- Basemap switching across multiple MapLibre style URLs.
- Vector drawing toolbar for point, line, and polygon sketching.
- WMTS/XYZ raster tile layer extension.
- WMS raster tile layer extension.
- MVT vector tile source extension.

## 3D Support

TerraTrace currently targets 2D and 2.5D map workflows. MapLibre can support camera pitch, rotation, and style-driven 3D-like layers such as fill extrusion when the source data has height attributes.

For true 3D scenes such as globe rendering, terrain-heavy visualization, BIM, oblique photography, or 3D Tiles, use a dedicated 3D module based on Cesium, Unity, or Unreal beside this 2D map SDK.

## Android SDK Usage

For local development, include the library module:

```kotlin
dependencies {
    implementation(project(":terra-trace-map"))
}
```

Use the map component in XML:

```xml
<com.terratrace.map.TerraTraceMapView
    android:id="@+id/terraTraceMap"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Initialize it from Kotlin:

```kotlin
terraTraceMap.initialize(
    savedInstanceState = savedInstanceState,
    config = TerraTraceMapConfig(),
    listener = this
)
```

React Native and Flutter wrappers should sit above `terra-trace-map`, not replace it.

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
