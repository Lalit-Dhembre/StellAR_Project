package com.cosmic_struck.stellar.stellar.pdfar.presentation

import com.cosmic_struck.stellar.stellar.pdfar.data.models.Concept
import com.cosmic_struck.stellar.stellar.pdfar.data.models.NativeImage

sealed interface PdfArUiState {
    object Idle : PdfArUiState
    object Uploading : PdfArUiState
    data class ContentLoaded(val concepts: List<Concept>, val nativeImages: List<NativeImage>) : PdfArUiState
    object GeneratingModel : PdfArUiState
    data class ModelReady(val modelUrl: String, val entityName: String, val script: String? = null) : PdfArUiState
    data class Error(val message: String) : PdfArUiState
}
