package com.cosmic_struck.stellar.classroom.presentation.viewmodel

import com.cosmic_struck.stellar.classroom.data.dto.ChatMessage

/**
 * UI state for the chatbot screen.
 */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val context: String = ""  // Document text for chat context
)
