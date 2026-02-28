package com.cosmic_struck.stellar.stellar.scantext.data.remote

import com.cosmic_struck.stellar.stellar.scantext.data.dto.JobStatusDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.PdfUploadDTO
import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ScanService {
    // Legacy image scan endpoint
    @Multipart
    @POST("/api/scan")
    suspend fun getScanResults(
        @Part image: MultipartBody.Part
    ) : ScanDTO

    // PDF Pipeline endpoints
    @Multipart
    @POST("/api/pdf/upload")
    suspend fun uploadPdf(
        @Part file: MultipartBody.Part,
        @Part("domain") domain: RequestBody
    ): PdfUploadDTO

    @GET("/api/pdf/status/{jobId}")
    suspend fun getJobStatus(
        @Path("jobId") jobId: String
    ): JobStatusDTO
}