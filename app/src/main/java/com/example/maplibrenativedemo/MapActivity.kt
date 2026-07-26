package com.example.maplibrenativedemo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.terratrace.map.TerraTraceDrawMode
import com.terratrace.map.TerraTraceFeature
import com.terratrace.map.TerraTraceLayerIds
import com.terratrace.map.TerraTraceMapConfig
import com.terratrace.map.TerraTraceMapController
import com.terratrace.map.TerraTraceMapListener
import com.terratrace.map.TerraTraceMapView
import com.terratrace.map.TerraTraceMeasureMode

class MapActivity : Activity(), TerraTraceMapListener {
    private lateinit var terraTraceMap: TerraTraceMapView
    private lateinit var statusText: TextView
    private var controller: TerraTraceMapController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        findViewById<Button>(R.id.closeMapButton).setOnClickListener {
            finish()
        }

        statusText = findViewById(R.id.statusText)
        terraTraceMap = findViewById(R.id.terraTraceMap)
        terraTraceMap.initialize(
            savedInstanceState = savedInstanceState,
            config = TerraTraceMapConfig(),
            listener = this
        )
    }

    override fun onMapReady(controller: TerraTraceMapController) {
        this.controller = controller
        controller.loadDemoLayers()
        statusText.setText(R.string.map_ready)
        bindLayerToggles(controller)
        bindMapTools(controller)
    }

    override fun onFeatureClick(feature: TerraTraceFeature) {
        AlertDialog.Builder(this)
            .setTitle(feature.title)
            .setMessage(
                buildString {
                    append(feature.description)
                    append("\n\nLng/Lat: ")
                    append("%.5f, %.5f".format(feature.lng, feature.lat))
                }
            )
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onMapClick(lng: Double, lat: Double) {
        statusText.text = "Map click: %.5f, %.5f".format(lng, lat)
    }

    override fun onDrawChanged(mode: TerraTraceDrawMode, pointCount: Int) {
        statusText.text = "Draw mode: ${mode.name}, points: $pointCount"
    }

    override fun onMeasureChanged(summary: String) {
        statusText.text = summary
    }

    override fun onPlaybackChanged(progress: Int, total: Int) {
        statusText.text = "Track playback: $progress / $total"
    }

    override fun onMapError(message: String) {
        statusText.text = message
    }

    private fun bindLayerToggles(controller: TerraTraceMapController) {
        bindToggle(R.id.trackCheck, TerraTraceLayerIds.TRACK, controller)
        bindToggle(R.id.pointsCheck, TerraTraceLayerIds.POINTS, controller)
        bindToggle(R.id.polygonCheck, TerraTraceLayerIds.POLYGON, controller)
        bindToggle(R.id.wmtsCheck, TerraTraceLayerIds.WMTS_RASTER, controller)
        bindToggle(R.id.wmsCheck, TerraTraceLayerIds.WMS_RASTER, controller)
        bindToggle(R.id.mvtCheck, TerraTraceLayerIds.MVT_FILL, controller)
    }

    private fun bindToggle(
        checkBoxId: Int,
        layerId: String,
        controller: TerraTraceMapController
    ) {
        findViewById<CheckBox>(checkBoxId).setOnCheckedChangeListener { _, isChecked ->
            controller.setLayerVisible(layerId, isChecked)
        }
    }

    private fun bindMapTools(controller: TerraTraceMapController) {
        findViewById<Button>(R.id.zoomInButton).setOnClickListener {
            controller.zoomIn()
        }
        findViewById<Button>(R.id.zoomOutButton).setOnClickListener {
            controller.zoomOut()
        }
        findViewById<Button>(R.id.baseMapButton).setOnClickListener {
            controller.switchBaseMap()
            statusText.text = "Base map switched"
        }
        findViewById<Button>(R.id.locationButton).setOnClickListener {
            locateUser()
        }
        findViewById<Button>(R.id.drawPointButton).setOnClickListener {
            controller.setDrawMode(TerraTraceDrawMode.POINT)
        }
        findViewById<Button>(R.id.drawLineButton).setOnClickListener {
            controller.setDrawMode(TerraTraceDrawMode.LINE)
        }
        findViewById<Button>(R.id.drawPolygonButton).setOnClickListener {
            controller.setDrawMode(TerraTraceDrawMode.POLYGON)
        }
        findViewById<Button>(R.id.drawStopButton).setOnClickListener {
            controller.setDrawMode(TerraTraceDrawMode.NONE)
        }
        findViewById<Button>(R.id.drawUndoButton).setOnClickListener {
            controller.undoDrawPoint()
        }
        findViewById<Button>(R.id.drawClearButton).setOnClickListener {
            controller.clearDrawing()
        }
        findViewById<Button>(R.id.measureDistanceButton).setOnClickListener {
            controller.setMeasureMode(TerraTraceMeasureMode.DISTANCE)
        }
        findViewById<Button>(R.id.measureAreaButton).setOnClickListener {
            controller.setMeasureMode(TerraTraceMeasureMode.AREA)
        }
        findViewById<Button>(R.id.measureClearButton).setOnClickListener {
            controller.clearMeasure()
            controller.setMeasureMode(TerraTraceMeasureMode.NONE)
        }
        findViewById<Button>(R.id.exportGeoJsonButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Drawing GeoJSON")
                .setMessage(controller.exportDrawingGeoJson())
                .setPositiveButton("Close", null)
                .show()
        }
        findViewById<Button>(R.id.playTrackButton).setOnClickListener {
            controller.startTrackPlayback()
        }
        findViewById<Button>(R.id.pauseTrackButton).setOnClickListener {
            controller.pauseTrackPlayback()
        }
        findViewById<Button>(R.id.resetTrackButton).setOnClickListener {
            controller.resetTrackPlayback()
        }
    }

    private fun locateUser() {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        val location = getLastKnownLocation()
        if (location == null) {
            statusText.text = "No location yet. Enable location services and try again."
        } else {
            controller?.showUserLocation(location.longitude, location.latitude)
            statusText.text = "Located: %.5f, %.5f".format(location.longitude, location.latitude)
        }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        return providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                locateUser()
            } else {
                statusText.text = "Location permission denied"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        terraTraceMap.onStart()
    }

    override fun onResume() {
        super.onResume()
        terraTraceMap.onResume()
    }

    override fun onPause() {
        terraTraceMap.onPause()
        super.onPause()
    }

    override fun onStop() {
        terraTraceMap.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        terraTraceMap.onLowMemory()
    }

    override fun onDestroy() {
        terraTraceMap.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        terraTraceMap.onSaveInstanceState(outState)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }
}
