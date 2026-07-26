package com.terratrace.map

interface TerraTraceMapController {
    fun loadDemoLayers()
    fun setLayerVisible(layerId: String, visible: Boolean)
    fun setLayerOpacity(layerId: String, opacity: Float)
    fun moveCamera(lng: Double, lat: Double, zoom: Double)
    fun addWmsLayer(tileTemplate: String)
    fun addWmtsLayer(tileTemplate: String)
    fun addVectorTileLayer(tileTemplate: String, sourceLayer: String)
}

