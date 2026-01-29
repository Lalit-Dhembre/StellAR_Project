package com.cosmic_struck.stellar.classroom.presentation.viewmodel.delegate

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ChatMessage
import com.cosmic_struck.stellar.classroom.domain.usecase.SendChatMessageUseCase
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ChatState
import com.cosmic_struck.stellar.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Delegate for handling chatbot state and API communication.
 */
class ChatDelegate @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase
) {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    /**
     * Set the document context for the chatbot.
     */
    fun setContext(context: String) {
        _state.value = _state.value.copy(context = context)
        Log.d("ChatDelegate", "Context set: ${context.take(100)}...")
    }

    /**
     * Send a message to the chatbot.
     */
    suspend fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        // Add user message to the list
        val userMessage = ChatMessage(role = "user", content = message)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            error = null
        )
        
        // Get history (all messages except the one we just added)
        val history = _state.value.messages.dropLast(1)
        
        // Send to API
        sendChatMessageUseCase(
            context = _state.value.context,
            message = message,
            history = history
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
                is Resource.Success -> {
                    val assistantMessage = ChatMessage(
                        role = "assistant",
                        content = result.data?.response ?: "No response received"
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        messages = _state.value.messages + assistantMessage
                    )
                    Log.d("ChatDelegate", "Response received")
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "Unknown error occurred"
                    )
                    Log.e("ChatDelegate", "Error: ${result.message}")
                }
            }
        }
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Reset the chat to initial state.
     */
    fun reset() {
        _state.value = ChatState()
    }
}
