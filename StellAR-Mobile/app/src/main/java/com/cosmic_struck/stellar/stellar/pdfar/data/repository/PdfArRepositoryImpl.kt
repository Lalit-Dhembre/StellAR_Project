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

class PdfArRepositoryImpl @Inject constructor(
    private val api: PdfArService
) : PdfArRepository {

    override suspend fun processPdf(pdfFile: File, domain: String): Result<ProcessPdfResponse> {
        return try {
            val requestFile = pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", pdfFile.name, requestFile)
            val domainBody = domain.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = api.processPdf(body, domainBody)
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null && data.success) {
                    Result.success(data)
                } else {
                    Result.failure(Exception(data?.error ?: "Invalid PDF"))
                }
            } else {
                Result.failure(Exception("Failed to process PDF: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConceptDetails(conceptId: String): Result<ConceptDetailsResponse> {
        return try {
            val response = api.getConceptDetails(ConceptDetailsRequest(conceptId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get concept details: ${response.code()}"))
            }
        } catch (e: Exception) {
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
