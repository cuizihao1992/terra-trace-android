package com.example.maplibrenativedemo

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.Property.LINE_CAP_ROUND
import org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

class MapActivity : Activity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_map)

        findViewById<Button>(R.id.closeMapButton).setOnClickListener {
            finish()
        }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapLibreMap: MapLibreMap) {
        map = mapLibreMap
        mapLibreMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(31.2304, 121.4737))
            .zoom(11.0)
            .build()

        mapLibreMap.setStyle(Style.Builder().fromUri(BASE_STYLE_URL)) { style ->
            addSampleTrack(style)
            addSamplePoints(style)
            wireFeaturePopup(mapLibreMap)
            // Extension point: call addWmsLayer(style), addWmtsLayer(style), or addVectorTileLayer(style).
        }
    }

    private fun addSampleTrack(style: Style) {
        val track = Feature.fromGeometry(
            LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(121.3930, 31.2075),
                    Point.fromLngLat(121.4300, 31.2200),
                    Point.fromLngLat(121.4737, 31.2304),
                    Point.fromLngLat(121.5150, 31.2450),
                    Point.fromLngLat(121.5520, 31.2220)
                )
            )
        )
        track.addStringProperty("name", "示例轨迹")
        track.addStringProperty("type", "LineString")

        style.addSource(GeoJsonSource(TRACK_SOURCE_ID, FeatureCollection.fromFeature(track)))
        style.addLayer(
            LineLayer(TRACK_LAYER_ID, TRACK_SOURCE_ID).withProperties(
                lineColor("#2563EB"),
                lineWidth(5f),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND)
            )
        )
    }

    private fun addSamplePoints(style: Style) {
        val features = listOf(
            pointFeature(121.4300, 31.2200, "轨迹节点 A", "车辆经过点"),
            pointFeature(121.4737, 31.2304, "中心点", "点击要素弹出详情"),
            pointFeature(121.5150, 31.2450, "轨迹节点 B", "后续可替换成业务 POI")
        )

        style.addSource(GeoJsonSource(POINT_SOURCE_ID, FeatureCollection.fromFeatures(features)))
        style.addLayer(
            CircleLayer(POINT_LAYER_ID, POINT_SOURCE_ID).withProperties(
                circleRadius(8f),
                circleColor("#F97316"),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2f)
            )
        )
    }

    private fun wireFeaturePopup(mapLibreMap: MapLibreMap) {
        mapLibreMap.addOnMapClickListener { latLng ->
            val screenPoint = mapLibreMap.projection.toScreenLocation(latLng)
            val features = mapLibreMap.queryRenderedFeatures(
                screenPoint,
                POINT_LAYER_ID,
                TRACK_LAYER_ID
            )

            if (features.isNotEmpty()) {
                val feature = features.first()
                val title = feature.getStringProperty("name") ?: "地图要素"
                val message = buildString {
                    append(feature.getStringProperty("description") ?: "没有描述")
                    append("\n\n经纬度: ")
                    append("%.5f, %.5f".format(latLng.longitude, latLng.latitude))
                }
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("关闭", null)
                    .show()
                true
            } else {
                false
            }
        }
    }

    private fun pointFeature(lng: Double, lat: Double, name: String, description: String): Feature {
        return Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
            addStringProperty("name", name)
            addStringProperty("description", description)
        }
    }

    @Suppress("unused")
    private fun addWmsLayer(style: Style) {
        // Add a RasterSource + RasterLayer here for WMS. Keep the server behind an XYZ-style proxy if possible.
    }

    @Suppress("unused")
    private fun addWmtsLayer(style: Style) {
        // Add a RasterSource + RasterLayer here for WMTS/XYZ tiles.
    }

    @Suppress("unused")
    private fun addVectorTileLayer(style: Style) {
        // Add a VectorSource + FillLayer/LineLayer/SymbolLayer here for MVT tiles.
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val BASE_STYLE_URL = "https://demotiles.maplibre.org/style.json"
        private const val TRACK_SOURCE_ID = "sample-track-source"
        private const val TRACK_LAYER_ID = "sample-track-layer"
        private const val POINT_SOURCE_ID = "sample-point-source"
        private const val POINT_LAYER_ID = "sample-point-layer"
    }
}

