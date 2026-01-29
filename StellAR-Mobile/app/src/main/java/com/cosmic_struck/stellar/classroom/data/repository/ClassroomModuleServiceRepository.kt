package com.cosmic_struck.stellar.classroom.data.repository

import com.cosmic_struck.stellar.classroom.data.dto.ChatMessage
import com.cosmic_struck.stellar.classroom.data.dto.ChatResponse
import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ClassroomModuleServiceRepository {
    suspend fun getProcessResponse(
        description: String,
        file: MultipartBody.Part
    ): ProcessResponse

    suspend fun sendChatMessage(
        context: String,
        message: String,
        history: List<ChatMessage>
    ): ChatResponse
}