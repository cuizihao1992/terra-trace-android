package com.terratrace.map

data class TerraTraceMapConfig(
    val styleUrl: String = "https://demotiles.maplibre.org/style.json",
    val initialLng: Double = 121.4737,
    val initialLat: Double = 31.2304,
    val initialZoom: Double = 11.0
)

