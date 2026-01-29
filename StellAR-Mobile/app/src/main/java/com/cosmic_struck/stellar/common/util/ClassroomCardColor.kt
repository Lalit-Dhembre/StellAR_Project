package com.cosmic_struck.stellar.common.util

import android.util.Log
import androidx.compose.ui.graphics.Color

fun getClassroomColor(classroomId: String): Color {
    val colors = listOf(
        Color(0xFFFF4081),
        Color(0xFF00B0FF),
        Color(0xFF00E676),
        Color(0xFFFFC400),
        Color(0xFF7C4DFF),
        Color(0xFF18FFFF),
        Color(0xFFFF6D00),
        Color(0xFF76FF03),
        Color(0xFF536DFE),
        Color(0xFFFF1744),
        Color(0xFF1DE9B6),
        Color(0xFFFF9100)
    )

    val index = kotlin.math.abs(classroomId.hashCode()) % colors.size
    return colors[index]
}
