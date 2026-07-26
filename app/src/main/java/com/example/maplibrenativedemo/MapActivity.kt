package com.example.maplibrenativedemo

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.terratrace.map.TerraTraceFeature
import com.terratrace.map.TerraTraceLayerIds
import com.terratrace.map.TerraTraceMapConfig
import com.terratrace.map.TerraTraceMapController
import com.terratrace.map.TerraTraceMapListener
import com.terratrace.map.TerraTraceMapView

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
}
