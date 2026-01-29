package com.cosmic_struck.stellar.stellar.scantext.presentation.viewmodel

import androidx.camera.core.ImageCapture
import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO

data class ScanImageScreenState(
    val isLoading: Boolean = false,
    val isError: String = "",
    val imageCapture: ImageCapture = ImageCapture.Builder().setCaptureMode(
        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
    ).build(),
    val image : String? = null,
    val scanResults : ScanDTO? = null,
    val count: Int = 0,
    val switchToResults : Boolean = false
)