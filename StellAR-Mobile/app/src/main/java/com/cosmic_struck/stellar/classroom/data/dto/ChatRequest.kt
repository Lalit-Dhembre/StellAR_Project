package com.cosmic_struck.stellar.classroom.data.dto

/**
 * Request body for chat API endpoint.
 * @param context The document text to answer questions about
 * @param message The user's current question
 * @param history List of previous conversation turns
 */
data class ChatRequest(
    val context: String,
    val message: String,
    val history: List<ChatMessage> = emptyList()
)
