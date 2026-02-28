package com.cosmic_struck.stellar.stellar.scantext.data.repository

import com.cosmic_struck.stellar.stellar.scantext.data.dto.JobStatusDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.PdfUploadDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO
import com.cosmic_struck.stellar.stellar.scantext.data.remote.ScanService
import com.cosmic_struck.stellar.stellar.scantext.domain.repository.ScanImageRepo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class ScanImageRepoImpl @Inject constructor(
    private val api: ScanService
) : ScanImageRepo {
    override suspend fun uploadImageToServer(image: MultipartBody.Part): ScanDTO {
        return api.getScanResults(image)
    }

    override suspend fun uploadPdfToServer(
        file: MultipartBody.Part,
        domain: RequestBody
    ): PdfUploadDTO {
        return api.uploadPdf(file, domain)
    }

    override suspend fun getJobStatus(jobId: String): JobStatusDTO {
        return api.getJobStatus(jobId)
    }
}