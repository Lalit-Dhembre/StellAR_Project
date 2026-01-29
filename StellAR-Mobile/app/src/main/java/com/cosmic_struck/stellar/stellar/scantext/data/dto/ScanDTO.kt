package com.cosmic_struck.stellar.stellar.scantext.data.dto

data class ScanDTO(
    val best_match: String,
    val count: Int,
    val detections: List<Detection>,
    val info: List<Info>,
    val success: Boolean
)
data class Info(
    val facts: List<String>,
    val summary: String,
    val badge: String,
    val title: String
)

data class Detection(
    val bbox: Bbox,
    val confidence: Double,
    val name: String
)
data class Bbox(
    val height: Int,
    val width: Int,
    val x1: Int,
    val x2: Int,
    val y1: Int,
    val y2: Int
)
