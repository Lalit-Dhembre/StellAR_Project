package com.cosmic_struck.stellar.classroom.data.repository

import com.cosmic_struck.stellar.classroom.data.dto.ChatMessage
import com.cosmic_struck.stellar.classroom.data.dto.ChatRequest
import com.cosmic_struck.stellar.classroom.data.dto.ChatResponse
import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import com.cosmic_struck.stellar.classroom.data.service.ClassroomModuleService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class ClassroomModuleServiceRepositoryImpl @Inject constructor(
    private val classroomModuleService: ClassroomModuleService
) : ClassroomModuleServiceRepository {
    override suspend fun getProcessResponse(
        description: String,
        file: MultipartBody.Part
    ): ProcessResponse {
        val descriptionBody = RequestBody.create("text/plain".toMediaType(), description)
        return classroomModuleService.processPdf(descriptionBody, file)
    }

    override suspend fun sendChatMessage(
        context: String,
        message: String,
        history: List<ChatMessage>
    ): ChatResponse {
        return classroomModuleService.sendChatMessage(
            ChatRequest(context, message, history)
        )
    }
}