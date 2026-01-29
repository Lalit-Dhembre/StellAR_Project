package com.cosmic_struck.stellar.stellar.scantext.data.remote

import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ScanService {
    @Multipart
    @POST("/api/scan")
    suspend fun getScanResults(
        @Part image: MultipartBody.Part
    ) : ScanDTO
}