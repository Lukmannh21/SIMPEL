// WitelModel.kt
package com.mbkm.telgo

data class WitelModel(
    val name: String,
    val address: String,
    val provinceCoordinates: Pair<Double, Double> // latitude, longitude
)