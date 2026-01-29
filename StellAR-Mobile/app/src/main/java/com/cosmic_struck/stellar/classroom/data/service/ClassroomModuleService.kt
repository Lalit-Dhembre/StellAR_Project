package com.cosmic_struck.stellar.classroom.data.service

import com.cosmic_struck.stellar.classroom.data.dto.ChatRequest
import com.cosmic_struck.stellar.classroom.data.dto.ChatResponse
import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ClassroomModuleService {
    @Multipart
    @POST("/api/module/process")
    suspend fun processPdf(
        @Part("description") description: RequestBody,
        @Part file: MultipartBody.Part
    ): ProcessResponse

    @POST("/api/module/chat")
    suspend fun sendChatMessage(
        @Body request: ChatRequest
    ): ChatResponse
}