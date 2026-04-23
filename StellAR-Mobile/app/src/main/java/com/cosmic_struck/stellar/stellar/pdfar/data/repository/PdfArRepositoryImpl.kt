package com.cosmic_struck.stellar.stellar.pdfar.data.repository

import com.cosmic_struck.stellar.stellar.pdfar.data.models.ProcessPdfResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ConceptDetailsRequest
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ConceptDetailsResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ResolveEntityRequest
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ResolveEntityResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.TaskStatusResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.remote.PdfArService
import com.cosmic_struck.stellar.stellar.pdfar.domain.repository.PdfArRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

import android.util.Log

class PdfArRepositoryImpl @Inject constructor(
    private val api: PdfArService
) : PdfArRepository {

    companion object {
        private const val TAG = "PdfArRepo"
    }

    override suspend fun processPdf(pdfFile: File, domain: String): Result<ProcessPdfResponse> {
        return try {
            Log.d(TAG, "processPdf: file=${pdfFile.name}, size=${pdfFile.length()}, domain=$domain")
            val requestFile = pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", pdfFile.name, requestFile)
            val domainBody = domain.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = api.processPdf(body, domainBody)
            Log.d(TAG, "processPdf: HTTP ${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful) {
                val data = response.body()
                Log.d(TAG, "processPdf body: success=${data?.success}, concepts=${data?.concepts?.size}, error=${data?.error}")
                if (data != null && data.success) {
                    Result.success(data)
                } else {
                    Result.failure(Exception(data?.error ?: "Invalid PDF"))
                }
            } else {
                val errorBody = response.errorBody()?.string()?.take(500)
                Log.e(TAG, "processPdf FAILED: HTTP ${response.code()}, body=$errorBody")
                Result.failure(Exception("Failed to process PDF: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "processPdf EXCEPTION", e)
            Result.failure(e)
        }
    }

    override suspend fun getConceptDetails(conceptId: String): Result<ConceptDetailsResponse> {
        return try {
            Log.d(TAG, "getConceptDetails: conceptId=$conceptId")
            val response = api.getConceptDetails(ConceptDetailsRequest(conceptId))
            Log.d(TAG, "getConceptDetails: HTTP ${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "getConceptDetails body: title=${body.title}, status=${body.modelStatus}, url=${body.modelUrl}")
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()?.take(500)
                Log.e(TAG, "getConceptDetails FAILED: HTTP ${response.code()}, body=$errorBody")
                Result.failure(Exception("Failed to get concept details: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getConceptDetails EXCEPTION for id=$conceptId", e)
            Result.failure(e)
        }
    }

    override suspend fun resolveEntity(entityName: String): Result<ResolveEntityResponse> {
        return try {
            val response = api.resolveEntity(ResolveEntityRequest(entityName))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to resolve entity: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaskStatus(taskId: String): Result<TaskStatusResponse> {
        return try {
            val response = api.getTaskStatus(taskId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get task status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadModel(url: String, destinationFile: File): Result<File> {
        return try {
            val response = api.downloadModel(url)
            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.IO) {
                    val body = response.body()!!
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(destinationFile)
                    
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                Result.success(destinationFile)
            } else {
                Result.failure(Exception("Failed to download model: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
