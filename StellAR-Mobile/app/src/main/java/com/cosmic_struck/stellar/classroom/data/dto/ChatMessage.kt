package com.cosmic_struck.stellar.classroom.data.dto

/**
 * Model for conversation history in chat.
 * @param role Either "user" or "assistant"
 * @param content The message content
 */
data class ChatMessage(
    val role: String,
    val content: String
)
