package com.cosmic_struck.stellar.stellar.scantext.domain.repository

import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO
import okhttp3.MultipartBody

interface ScanImageRepo {
    suspend fun uploadImageToServer(
        image: MultipartBody.Part
    ) : ScanDTO
}