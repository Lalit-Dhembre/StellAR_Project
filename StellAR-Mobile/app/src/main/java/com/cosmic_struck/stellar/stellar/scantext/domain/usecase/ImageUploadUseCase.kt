package com.cosmic_struck.stellar.stellar.scantext.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.stellar.scantext.data.dto.ScanDTO
import com.cosmic_struck.stellar.stellar.scantext.domain.repository.ScanImageRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import javax.inject.Inject

class ImageUploadUseCase @Inject constructor(
    private val scanImageRepo: ScanImageRepo
) {
    operator fun invoke(image: MultipartBody.Part): Flow<Resource<ScanDTO>> = flow {
        try {
            emit(Resource.Loading())
            Log.d("ImageUploadUseCase", "Uploading image...")
            val scanDTO = scanImageRepo.uploadImageToServer(image)
            Log.d("ImageUploadUseCase", "Image uploaded successfully: $scanDTO")
            emit(Resource.Success(scanDTO))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
            Log.e("ImageUploadUseCase", "Image uploaded successfully: ${e.message}")
        }
    }
}