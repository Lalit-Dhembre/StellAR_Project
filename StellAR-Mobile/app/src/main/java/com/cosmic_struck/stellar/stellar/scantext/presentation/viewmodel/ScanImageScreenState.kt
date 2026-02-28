package com.cosmic_struck.stellar.stellar.scantext.presentation.viewmodel

import android.net.Uri
import com.cosmic_struck.stellar.stellar.scantext.data.dto.JobStatusDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.PdfUploadDTO

data class ScanImageScreenState(
    val isLoading: Boolean = false,
    val isError: String = "",
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val domain: String = "any",
    val pdfResponse: PdfUploadDTO? = null,
    val jobStatus: JobStatusDTO? = null,
    val isPolling: Boolean = false,
    val switchToResults: Boolean = false
)