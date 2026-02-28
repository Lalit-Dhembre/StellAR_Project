package com.cosmic_struck.stellar.stellar.scantext.domain.repository

import com.cosmic_struck.stellar.stellar.scantext.data.dto.JobStatusDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.PdfUploadDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ScanImageRepo {
    suspend fun uploadImageToServer(
        image: MultipartBody.Part
    ) : ScanDTO

    suspend fun uploadPdfToServer(
        file: MultipartBody.Part,
        domain: RequestBody
    ): PdfUploadDTO

    suspend fun getJobStatus(jobId: String): JobStatusDTO
}