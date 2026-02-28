package com.cosmic_struck.stellar.stellar.scantext.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.stellar.scantext.data.dto.PdfUploadDTO
import com.cosmic_struck.stellar.stellar.scantext.domain.repository.ScanImageRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class PdfUploadUseCase @Inject constructor(
    private val scanImageRepo: ScanImageRepo
) {
    operator fun invoke(
        file: MultipartBody.Part,
        domain: RequestBody
    ): Flow<Resource<PdfUploadDTO>> = flow {
        try {
            emit(Resource.Loading())
            Log.d("PdfUploadUseCase", "Uploading PDF...")
            val response = scanImageRepo.uploadPdfToServer(file, domain)
            Log.d("PdfUploadUseCase", "PDF upload response: success=${response.success}")

            if (response.rejected == true) {
                Log.d("PdfUploadUseCase", "Document rejected: ${response.reason}")
                emit(Resource.Error(response.reason ?: "Document was rejected"))
            } else {
                emit(Resource.Success(response))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
            Log.e("PdfUploadUseCase", "PDF upload failed: ${e.message}")
        }
    }
}
