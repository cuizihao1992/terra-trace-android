package com.terratrace.map

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

class TerraTraceMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), TerraTraceMapController {
    private val mapView: MapView
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var listener: TerraTraceMapListener? = null
    private var config: TerraTraceMapConfig = TerraTraceMapConfig()
    private var baseMapIndex = 0
    private var demoLayersLoaded = false
    private var drawMode = TerraTraceDrawMode.NONE
    private val drawPoints = mutableListOf<Point>()
    private var measureMode = TerraTraceMeasureMode.NONE
    private val measurePoints = mutableListOf<Point>()
    private val playbackHandler = Handler(Looper.getMainLooper())
    private var playbackIndex = 0
    private var playbackRunning = false
    private val sampleTrackPoints = listOf(
        Point.fromLngLat(121.3930, 31.2075),
        Point.fromLngLat(121.4300, 31.2200),
        Point.fromLngLat(121.4737, 31.2304),
        Point.fromLngLat(121.5150, 31.2450),
        Point.fromLngLat(121.5520, 31.2220)
    )

    init {
        MapLibre.getInstance(context.applicationContext)
        mapView = MapView(context)
        addView(
            mapView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun initialize(
        savedInstanceState: Bundle?,
        config: TerraTraceMapConfig = TerraTraceMapConfig(),
        listener: TerraTraceMapListener? = null
    ) {
        this.listener = listener
        this.config = config
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap
            mapLibreMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(config.initialLat, config.initialLng))
                .zoom(config.initialZoom)
                .build()

            mapLibreMap.setStyle(Style.Builder().fromUri(config.styleUrl)) { loadedStyle ->
                style = loadedStyle
                wireClicks(mapLibreMap)
                listener?.onMapReady(this)
            }
        }
    }

    override fun loadDemoLayers() {
        val currentStyle = style ?: return
        demoLayersLoaded = true
        addDemoTrack(currentStyle)
        addDemoPoints(currentStyle)
        addDemoPolygon(currentStyle)
        ensureDrawingLayers(currentStyle)
        ensureMeasureLayers(currentStyle)
        ensurePlaybackLayer(currentStyle)
        addWmtsLayer(DEFAULT_WMTS_TEMPLATE)
        addWmsLayer(DEFAULT_WMS_TEMPLATE)
        addVectorTileLayer(DEFAULT_MVT_TEMPLATE, DEFAULT_MVT_SOURCE_LAYER)
        setLayerVisible(TerraTraceLayerIds.WMTS_RASTER, false)
        setLayerVisible(TerraTraceLayerIds.WMS_RASTER, false)
        setLayerVisible(TerraTraceLayerIds.MVT_FILL, false)
    }

    override fun setLayerVisible(layerId: String, visible: Boolean) {
        style?.getLayer(layerId)?.setProperties(
            visibility(if (visible) Property.VISIBLE else Property.NONE)
        )
    }

    override fun setLayerOpacity(layerId: String, opacity: Float) {
        style?.getLayer(layerId)?.let { layer ->
            if (layer is RasterLayer) {
                layer.setProperties(rasterOpacity(opacity))
            } else if (layer is FillLayer) {
                layer.setProperties(fillOpacity(opacity))
            }
        }
    }

    override fun moveCamera(lng: Double, lat: Double, zoom: Double) {
        map?.cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(zoom)
            .build()
    }

    override fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    override fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut())
    }

    override fun switchBaseMap() {
        val mapLibreMap = map ?: return
        baseMapIndex = (baseMapIndex + 1) % BASE_STYLES.size
        mapLibreMap.setStyle(Style.Builder().fromUri(BASE_STYLES[baseMapIndex])) { loadedStyle ->
            style = loadedStyle
            if (demoLayersLoaded) {
                loadDemoLayers()
            }
        }
    }

    override fun showUserLocation(lng: Double, lat: Double, zoom: Double) {
        val currentStyle = style ?: return
        val feature = Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
            addStringProperty("title", "User location")
            addStringProperty("description", "Location supplied by host app.")
            addStringProperty("layer", TerraTraceLayerIds.USER_LOCATION)
        }
        val source = currentStyle.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID)
        if (source == null) {
            currentStyle.addSource(GeoJsonSource(LOCATION_SOURCE_ID, FeatureCollection.fromFeature(feature)))
            currentStyle.addLayer(
                CircleLayer(TerraTraceLayerIds.USER_LOCATION, LOCATION_SOURCE_ID).withProperties(
                    circleRadius(10f),
                    circleColor("#10B981"),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(3f)
                )
            )
        } else {
            source.setGeoJson(FeatureCollection.fromFeature(feature))
        }
        moveCamera(lng, lat, zoom)
    }

    override fun setDrawMode(mode: TerraTraceDrawMode) {
        drawMode = mode
        listener?.onDrawChanged(drawMode, drawPoints.size)
    }

    override fun undoDrawPoint() {
        if (drawPoints.isNotEmpty()) {
            drawPoints.removeAt(drawPoints.lastIndex)
            updateDrawing()
        }
    }

    override fun clearDrawing() {
        drawPoints.clear()
        updateDrawing()
    }

    override fun exportDrawingGeoJson(): String {
        val features = mutableListOf<Feature>()
        drawPoints.forEachIndexed { index, point ->
            features.add(
                Feature.fromGeometry(point).apply {
                    addStringProperty("title", "Draw point ${index + 1}")
                }
            )
        }
        if (drawPoints.size >= 2) {
            features.add(
                Feature.fromGeometry(LineString.fromLngLats(drawPoints)).apply {
                    addStringProperty("title", "Draw line")
                }
            )
        }
        if (drawPoints.size >= 3) {
            val closed = drawPoints.toMutableList()
            closed.add(drawPoints.first())
            features.add(
                Feature.fromGeometry(Polygon.fromLngLats(listOf(closed))).apply {
                    addStringProperty("title", "Draw polygon")
                }
            )
        }
        return FeatureCollection.fromFeatures(features).toJson()
    }

    override fun setMeasureMode(mode: TerraTraceMeasureMode) {
        measureMode = mode
        drawMode = TerraTraceDrawMode.NONE
        listener?.onMeasureChanged(measureSummary())
    }

    override fun clearMeasure() {
        measurePoints.clear()
        updateMeasure()
    }

    override fun startTrackPlayback() {
        playbackRunning = true
        ensurePlaybackLayer(style ?: return)
        playbackHandler.removeCallbacksAndMessages(null)
        playbackHandler.post(playbackTick)
    }

    override fun pauseTrackPlayback() {
        playbackRunning = false
        playbackHandler.removeCallbacksAndMessages(null)
    }

    override fun resetTrackPlayback() {
        pauseTrackPlayback()
        playbackIndex = 0
        updatePlaybackPoint()
    }

    override fun addWmsLayer(tileTemplate: String) {
        val currentStyle = style ?: return
        if (currentStyle.getLayer(TerraTraceLayerIds.WMS_RASTER) != null) return
        val tileSet = TileSet("2.2.0", tileTemplate)
        currentStyle.addSource(RasterSource(WMS_SOURCE_ID, tileSet, 256))
        currentStyle.addLayer(
            RasterLayer(TerraTraceLayerIds.WMS_RASTER, WMS_SOURCE_ID).withProperties(
                rasterOpacity(0.55f)
            )
        )
    }

    override fun addWmtsLayer(tileTemplate: String) {
        val currentStyle = style ?: return
        if (currentStyle.getLayer(TerraTraceLayerIds.WMTS_RASTER) != null) return
        val tileSet = TileSet("2.2.0", tileTemplate)
        currentStyle.addSource(RasterSource(WMTS_SOURCE_ID, tileSet, 256))
        currentStyle.addLayer(
            RasterLayer(TerraTraceLayerIds.WMTS_RASTER, WMTS_SOURCE_ID).withProperties(
                rasterOpacity(0.45f)
            )
        )
    }

    override fun addVectorTileLayer(tileTemplate: String, sourceLayer: String) {
        val currentStyle = style ?: return
        if (currentStyle.getLayer(TerraTraceLayerIds.MVT_FILL) != null) return
        val tileSet = TileSet("2.2.0", tileTemplate)
        currentStyle.addSource(VectorSource(MVT_SOURCE_ID, tileSet))
        currentStyle.addLayer(
            FillLayer(TerraTraceLayerIds.MVT_FILL, MVT_SOURCE_ID)
                .withSourceLayer(sourceLayer)
                .withProperties(
                    fillColor("#14B8A6"),
                    fillOpacity(0.22f)
                )
        )
    }

    private fun addDemoTrack(currentStyle: Style) {
        if (currentStyle.getLayer(TerraTraceLayerIds.TRACK) != null) return
        val track = Feature.fromGeometry(
            LineString.fromLngLats(
                listOf(
                    sampleTrackPoints[0],
                    sampleTrackPoints[1],
                    sampleTrackPoints[2],
                    sampleTrackPoints[3],
                    sampleTrackPoints[4]
                )
            )
        )
        track.addStringProperty("title", "Sample track")
        track.addStringProperty("description", "LineLayer rendered from GeoJSON LineString.")
        track.addStringProperty("layer", TerraTraceLayerIds.TRACK)

        currentStyle.addSource(GeoJsonSource(TRACK_SOURCE_ID, FeatureCollection.fromFeature(track)))
        currentStyle.addLayer(
            LineLayer(TerraTraceLayerIds.TRACK, TRACK_SOURCE_ID).withProperties(
                lineColor("#2563EB"),
                lineWidth(5f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
    }

    private fun addDemoPoints(currentStyle: Style) {
        if (currentStyle.getLayer(TerraTraceLayerIds.POINTS) != null) return
        val features = listOf(
            pointFeature(121.4300, 31.2200, "Track node A", "Clickable GeoJSON point."),
            pointFeature(121.4737, 31.2304, "Center feature", "Tap to show a native popup."),
            pointFeature(121.5150, 31.2450, "Track node B", "Business POI placeholder.")
        )

        currentStyle.addSource(GeoJsonSource(POINT_SOURCE_ID, FeatureCollection.fromFeatures(features)))
        currentStyle.addLayer(
            CircleLayer(TerraTraceLayerIds.POINTS, POINT_SOURCE_ID).withProperties(
                circleRadius(8f),
                circleColor("#F97316"),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2f)
            )
        )
    }

    private fun addDemoPolygon(currentStyle: Style) {
        if (currentStyle.getLayer(TerraTraceLayerIds.POLYGON) != null) return
        val polygon = Feature.fromGeometry(
            Polygon.fromLngLats(
                listOf(
                    listOf(
                        Point.fromLngLat(121.4550, 31.2050),
                        Point.fromLngLat(121.5050, 31.2080),
                        Point.fromLngLat(121.5150, 31.2550),
                        Point.fromLngLat(121.4500, 31.2500),
                        Point.fromLngLat(121.4550, 31.2050)
                    )
                )
            )
        )
        polygon.addStringProperty("title", "Demo area")
        polygon.addStringProperty("description", "FillLayer rendered from GeoJSON Polygon.")
        polygon.addStringProperty("layer", TerraTraceLayerIds.POLYGON)

        currentStyle.addSource(GeoJsonSource(POLYGON_SOURCE_ID, FeatureCollection.fromFeature(polygon)))
        currentStyle.addLayerBelow(
            FillLayer(TerraTraceLayerIds.POLYGON, POLYGON_SOURCE_ID).withProperties(
                fillColor("#0F7B6C"),
                fillOpacity(0.20f)
            ),
            TerraTraceLayerIds.TRACK
        )
    }

    private fun wireClicks(mapLibreMap: MapLibreMap) {
        mapLibreMap.addOnMapClickListener { latLng ->
            if (drawMode != TerraTraceDrawMode.NONE) {
                drawPoints.add(Point.fromLngLat(latLng.longitude, latLng.latitude))
                updateDrawing()
                return@addOnMapClickListener true
            }
            if (measureMode != TerraTraceMeasureMode.NONE) {
                measurePoints.add(Point.fromLngLat(latLng.longitude, latLng.latitude))
                updateMeasure()
                return@addOnMapClickListener true
            }

            val screenPoint = mapLibreMap.projection.toScreenLocation(latLng)
            val features = mapLibreMap.queryRenderedFeatures(
                screenPoint,
                TerraTraceLayerIds.POINTS,
                TerraTraceLayerIds.TRACK,
                TerraTraceLayerIds.POLYGON,
                TerraTraceLayerIds.MVT_FILL
            )

            if (features.isNotEmpty()) {
                val feature = features.first()
                val properties = mutableMapOf<String, String>()
                feature.properties()?.entrySet()?.forEach { entry ->
                    properties[entry.key] = entry.value.toString()
                }
                listener?.onFeatureClick(
                    TerraTraceFeature(
                        layerId = feature.getStringProperty("layer") ?: "unknown",
                        title = feature.getStringProperty("title") ?: "Map feature",
                        description = feature.getStringProperty("description") ?: "No description.",
                        lng = latLng.longitude,
                        lat = latLng.latitude,
                        properties = properties
                    )
                )
                true
            } else {
                listener?.onMapClick(latLng.longitude, latLng.latitude)
                false
            }
        }
    }

    private fun ensureDrawingLayers(currentStyle: Style) {
        if (currentStyle.getSource(DRAW_POINT_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(DRAW_POINT_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayer(
                CircleLayer(TerraTraceLayerIds.DRAW_POINTS, DRAW_POINT_SOURCE_ID).withProperties(
                    circleRadius(6f),
                    circleColor("#111827"),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(2f)
                )
            )
        }
        if (currentStyle.getSource(DRAW_LINE_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(DRAW_LINE_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayer(
                LineLayer(TerraTraceLayerIds.DRAW_LINE, DRAW_LINE_SOURCE_ID).withProperties(
                    lineColor("#111827"),
                    lineWidth(3f),
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
        }
        if (currentStyle.getSource(DRAW_POLYGON_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(DRAW_POLYGON_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayerBelow(
                FillLayer(TerraTraceLayerIds.DRAW_POLYGON, DRAW_POLYGON_SOURCE_ID).withProperties(
                    fillColor("#F59E0B"),
                    fillOpacity(0.28f)
                ),
                TerraTraceLayerIds.DRAW_LINE
            )
        }
    }

    private fun ensureMeasureLayers(currentStyle: Style) {
        if (currentStyle.getSource(MEASURE_POINT_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(MEASURE_POINT_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayer(
                CircleLayer(MEASURE_POINT_LAYER_ID, MEASURE_POINT_SOURCE_ID).withProperties(
                    circleRadius(6f),
                    circleColor("#DC2626"),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(2f)
                )
            )
        }
        if (currentStyle.getSource(MEASURE_LINE_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(MEASURE_LINE_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayer(
                LineLayer(MEASURE_LINE_LAYER_ID, MEASURE_LINE_SOURCE_ID).withProperties(
                    lineColor("#DC2626"),
                    lineWidth(3f),
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
        }
        if (currentStyle.getSource(MEASURE_POLYGON_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(MEASURE_POLYGON_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayerBelow(
                FillLayer(MEASURE_POLYGON_LAYER_ID, MEASURE_POLYGON_SOURCE_ID).withProperties(
                    fillColor("#DC2626"),
                    fillOpacity(0.16f)
                ),
                MEASURE_LINE_LAYER_ID
            )
        }
    }

    private fun ensurePlaybackLayer(currentStyle: Style) {
        if (currentStyle.getSource(PLAYBACK_SOURCE_ID) == null) {
            currentStyle.addSource(GeoJsonSource(PLAYBACK_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
            currentStyle.addLayer(
                CircleLayer(TerraTraceLayerIds.PLAYBACK_POINT, PLAYBACK_SOURCE_ID).withProperties(
                    circleRadius(9f),
                    circleColor("#8B5CF6"),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(3f)
                )
            )
        }
    }

    private fun updateDrawing() {
        val currentStyle = style ?: return
        ensureDrawingLayers(currentStyle)
        val pointFeatures = drawPoints.mapIndexed { index, point ->
            Feature.fromGeometry(point).apply {
                addStringProperty("title", "Draw point ${index + 1}")
                addStringProperty("description", "Vector drawing point.")
                addStringProperty("layer", TerraTraceLayerIds.DRAW_POINTS)
            }
        }
        currentStyle.getSourceAs<GeoJsonSource>(DRAW_POINT_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(pointFeatures))

        val lineFeatures = if (drawPoints.size >= 2) {
            listOf(Feature.fromGeometry(LineString.fromLngLats(drawPoints)))
        } else {
            emptyList()
        }
        currentStyle.getSourceAs<GeoJsonSource>(DRAW_LINE_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(lineFeatures))

        val polygonFeatures = if (drawPoints.size >= 3) {
            val closed = drawPoints.toMutableList()
            closed.add(drawPoints.first())
            listOf(Feature.fromGeometry(Polygon.fromLngLats(listOf(closed))))
        } else {
            emptyList()
        }
        currentStyle.getSourceAs<GeoJsonSource>(DRAW_POLYGON_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures))
        listener?.onDrawChanged(drawMode, drawPoints.size)
    }

    private fun updateMeasure() {
        val currentStyle = style ?: return
        ensureMeasureLayers(currentStyle)
        currentStyle.getSourceAs<GeoJsonSource>(MEASURE_POINT_SOURCE_ID)
            ?.setGeoJson(
                FeatureCollection.fromFeatures(
                    measurePoints.mapIndexed { index, point ->
                        Feature.fromGeometry(point).apply {
                            addStringProperty("title", "Measure point ${index + 1}")
                        }
                    }
                )
            )

        val lineFeatures = if (measurePoints.size >= 2) {
            listOf(Feature.fromGeometry(LineString.fromLngLats(measurePoints)))
        } else {
            emptyList()
        }
        currentStyle.getSourceAs<GeoJsonSource>(MEASURE_LINE_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(lineFeatures))

        val polygonFeatures = if (measureMode == TerraTraceMeasureMode.AREA && measurePoints.size >= 3) {
            val closed = measurePoints.toMutableList()
            closed.add(measurePoints.first())
            listOf(Feature.fromGeometry(Polygon.fromLngLats(listOf(closed))))
        } else {
            emptyList()
        }
        currentStyle.getSourceAs<GeoJsonSource>(MEASURE_POLYGON_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures))
        listener?.onMeasureChanged(measureSummary())
    }

    private fun measureSummary(): String {
        return when (measureMode) {
            TerraTraceMeasureMode.NONE -> "Measure off"
            TerraTraceMeasureMode.DISTANCE -> "Distance: ${formatDistance(measureDistanceMeters())}, points: ${measurePoints.size}"
            TerraTraceMeasureMode.AREA -> "Area: ${formatArea(measureAreaSquareMeters())}, distance: ${formatDistance(measureDistanceMeters())}"
        }
    }

    private fun measureDistanceMeters(): Double {
        if (measurePoints.size < 2) return 0.0
        return measurePoints.zipWithNext().sumOf { (a, b) ->
            val results = FloatArray(1)
            Location.distanceBetween(a.latitude(), a.longitude(), b.latitude(), b.longitude(), results)
            results[0].toDouble()
        }
    }

    private fun measureAreaSquareMeters(): Double {
        if (measurePoints.size < 3) return 0.0
        val originLat = measurePoints.map { it.latitude() }.average()
        val metersPerDegreeLat = 111_320.0
        val metersPerDegreeLng = 111_320.0 * kotlin.math.cos(Math.toRadians(originLat))
        val projected = measurePoints.map {
            Pair(it.longitude() * metersPerDegreeLng, it.latitude() * metersPerDegreeLat)
        }
        var sum = 0.0
        projected.forEachIndexed { index, current ->
            val next = projected[(index + 1) % projected.size]
            sum += current.first * next.second - next.first * current.second
        }
        return kotlin.math.abs(sum) / 2.0
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            "%.2f km".format(meters / 1000.0)
        } else {
            "%.0f m".format(meters)
        }
    }

    private fun formatArea(squareMeters: Double): String {
        return if (squareMeters >= 1_000_000) {
            "%.2f km²".format(squareMeters / 1_000_000.0)
        } else {
            "%.0f m²".format(squareMeters)
        }
    }

    private val playbackTick = object : Runnable {
        override fun run() {
            if (!playbackRunning) return
            updatePlaybackPoint()
            playbackIndex = (playbackIndex + 1).coerceAtMost(sampleTrackPoints.lastIndex)
            if (playbackIndex >= sampleTrackPoints.lastIndex) {
                playbackRunning = false
            } else {
                playbackHandler.postDelayed(this, 700)
            }
        }
    }

    private fun updatePlaybackPoint() {
        val currentStyle = style ?: return
        ensurePlaybackLayer(currentStyle)
        val point = sampleTrackPoints[playbackIndex.coerceIn(0, sampleTrackPoints.lastIndex)]
        val feature = Feature.fromGeometry(point).apply {
            addStringProperty("title", "Playback")
            addStringProperty("description", "Track playback marker.")
        }
        currentStyle.getSourceAs<GeoJsonSource>(PLAYBACK_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeature(feature))
        listener?.onPlaybackChanged(playbackIndex + 1, sampleTrackPoints.size)
    }

    private fun pointFeature(lng: Double, lat: Double, title: String, description: String): Feature {
        return Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
            addStringProperty("title", title)
            addStringProperty("description", description)
            addStringProperty("layer", TerraTraceLayerIds.POINTS)
        }
    }

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onDestroy() {
        pauseTrackPlayback()
        mapView.onDestroy()
    }
    fun onLowMemory() = mapView.onLowMemory()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)

    companion object {
        private const val TRACK_SOURCE_ID = "terra-track-source"
        private const val POINT_SOURCE_ID = "terra-point-source"
        private const val POLYGON_SOURCE_ID = "terra-polygon-source"
        private const val WMS_SOURCE_ID = "terra-wms-source"
        private const val WMTS_SOURCE_ID = "terra-wmts-source"
        private const val MVT_SOURCE_ID = "terra-mvt-source"
        private const val LOCATION_SOURCE_ID = "terra-user-location-source"
        private const val DRAW_POINT_SOURCE_ID = "terra-draw-points-source"
        private const val DRAW_LINE_SOURCE_ID = "terra-draw-line-source"
        private const val DRAW_POLYGON_SOURCE_ID = "terra-draw-polygon-source"
        private const val MEASURE_POINT_SOURCE_ID = "terra-measure-points-source"
        private const val MEASURE_LINE_SOURCE_ID = "terra-measure-line-source"
        private const val MEASURE_POLYGON_SOURCE_ID = "terra-measure-polygon-source"
        private const val MEASURE_POINT_LAYER_ID = "terra-measure-points-layer"
        private const val MEASURE_LINE_LAYER_ID = "terra-measure-line-layer"
        private const val MEASURE_POLYGON_LAYER_ID = "terra-measure-polygon-layer"
        private const val PLAYBACK_SOURCE_ID = "terra-playback-source"

        private val BASE_STYLES = listOf(
            "https://demotiles.maplibre.org/style.json",
            "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json",
            "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
        )

        private const val DEFAULT_WMTS_TEMPLATE =
            "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_CityLights_2012/default/2012-01-01/GoogleMapsCompatible_Level8/{z}/{y}/{x}.jpg"
        private const val DEFAULT_WMS_TEMPLATE =
            "https://ahocevar.com/geoserver/wms?service=WMS&version=1.1.1&request=GetMap&layers=topp:states&styles=&bbox={bbox-epsg-3857}&width=256&height=256&srs=EPSG:3857&format=image/png&transparent=true"
        private const val DEFAULT_MVT_TEMPLATE =
            "https://demotiles.maplibre.org/tiles/{z}/{x}/{y}.pbf"
        private const val DEFAULT_MVT_SOURCE_LAYER = "countries"
    }
}
