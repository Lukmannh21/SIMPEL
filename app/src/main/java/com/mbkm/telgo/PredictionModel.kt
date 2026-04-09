package com.mbkm.telgo

data class PredictionModel(
    val siteId: String,
    val witel: String,
    val isHighRisk: Boolean,
    val probability: Double
)