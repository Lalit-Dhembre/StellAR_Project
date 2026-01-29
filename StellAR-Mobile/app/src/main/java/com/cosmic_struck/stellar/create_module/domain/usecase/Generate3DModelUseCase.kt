package com.cosmic_struck.stellar.create_module.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.create_module.data.service.ModelGenerationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class Generate3DModelUseCase @Inject constructor(
    private val service: ModelGenerationService
) {
    operator fun invoke(context: Context, imageUri: Uri): Flow<Resource<Uri>> = flow {
        emit(Resource.Loading())
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
                ?: throw Exception("Could not read image file")

            val bytes = inputStream.readBytes()
            inputStream.close()

            val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", "upload.jpg", requestFile)

            Log.d("Generate3DModelUseCase", "Sending request to generate model...")
            val response = service.generateModel(imagePart)

            if (response.isSuccessful && response.body() != null) {
                Log.d("Generate3DModelUseCase", "Response successful, saving file...")
                val responseBody = response.body()!!
                
                // Save to cache directory
                val outputDir = File(context.cacheDir, "generated_models")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                val outputFile = File(outputDir, "generated_model_${System.currentTimeMillis()}.glb")
                
                val outputStream = FileOutputStream(outputFile)
                outputStream.use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                Log.d("Generate3DModelUseCase", "File saved to ${outputFile.absolutePath}")
                emit(Resource.Success(Uri.fromFile(outputFile)))
            } else {
                Log.e("Generate3DModelUseCase", "API Error: ${response.code()} ${response.message()}")
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error("Generation failed: ${response.message()} ($errorBody)"))
            }
        } catch (e: Exception) {
            Log.e("Generate3DModelUseCase", "Exception: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
}
