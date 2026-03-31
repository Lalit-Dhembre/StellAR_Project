package com.cosmic_struck.stellar.stellar.pdfar.presentation

import com.cosmic_struck.stellar.stellar.pdfar.data.models.Section


sealed interface PdfArUiState {
    object Idle : PdfArUiState
    object Uploading : PdfArUiState
    data class SectionsLoaded(val sections: List<Section>) : PdfArUiState
    object GeneratingModel : PdfArUiState
    data class ModelReady(val modelUrl: String, val entityName: String) : PdfArUiState
    data class Error(val message: String) : PdfArUiState
}
