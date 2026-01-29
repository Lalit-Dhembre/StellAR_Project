package com.cosmic_struck.stellar.classroom.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ChatMessage
import com.cosmic_struck.stellar.classroom.data.dto.ChatResponse
import com.cosmic_struck.stellar.classroom.data.repository.ClassroomModuleServiceRepository
import com.cosmic_struck.stellar.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for sending chat messages to the AI chatbot.
 */
class SendChatMessageUseCase @Inject constructor(
    private val repository: ClassroomModuleServiceRepository
) {
    operator fun invoke(
        context: String,
        message: String,
        history: List<ChatMessage>
    ): Flow<Resource<ChatResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = repository.sendChatMessage(context, message, history)
            emit(Resource.Success(response))
            Log.d("ChatBot", "Response received: ${response.response.take(50)}...")
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to get chat response"))
            Log.e("ChatBot", "Error: ${e.message}")
        }
    }
}
