# TerraTrace Android

This is a small Android-native MapLibre app prototype for a frontend-oriented developer.

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
