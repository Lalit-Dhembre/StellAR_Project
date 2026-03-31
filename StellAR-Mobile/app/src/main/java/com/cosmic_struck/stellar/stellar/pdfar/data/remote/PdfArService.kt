package com.cosmic_struck.stellar.stellar.pdfar.data.remote

import com.cosmic_struck.stellar.stellar.pdfar.data.models.ProcessPdfResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ResolveEntityRequest
import com.cosmic_struck.stellar.stellar.pdfar.data.models.ResolveEntityResponse
import com.cosmic_struck.stellar.stellar.pdfar.data.models.TaskStatusResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface PdfArService {

    @Multipart
    @POST("/api/scan")
    suspend fun processPdf(
        @Part file: MultipartBody.Part,
        @Part("domain") domain: okhttp3.RequestBody
    ): Response<ProcessPdfResponse>

    @POST("/resolve-entity")
    suspend fun resolveEntity(
        @Body request: ResolveEntityRequest
    ): Response<ResolveEntityResponse>

    @GET("/task-status/{task_id}")
    suspend fun getTaskStatus(
        @Path("task_id") taskId: String
    ): Response<TaskStatusResponse>

    @Streaming
    @GET
    suspend fun downloadModel(
        @Url fileUrl: String
    ): Response<ResponseBody>
}
