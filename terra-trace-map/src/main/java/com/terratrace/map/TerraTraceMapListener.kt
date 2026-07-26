package com.terratrace.map

interface TerraTraceMapListener {
    fun onMapReady(controller: TerraTraceMapController) = Unit
    fun onFeatureClick(feature: TerraTraceFeature) = Unit
    fun onMapClick(lng: Double, lat: Double) = Unit
    fun onDrawChanged(mode: TerraTraceDrawMode, pointCount: Int) = Unit
    fun onMeasureChanged(summary: String) = Unit
    fun onPlaybackChanged(progress: Int, total: Int) = Unit
    fun onMapError(message: String) = Unit
}
