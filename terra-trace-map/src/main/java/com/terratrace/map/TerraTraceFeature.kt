package com.terratrace.map

data class TerraTraceFeature(
    val layerId: String,
    val title: String,
    val description: String,
    val lng: Double,
    val lat: Double,
    val properties: Map<String, String>
)

