package com.cosmic_struck.stellar.classroom.data.dto

/**
 * Response from chat API endpoint.
 * @param success Whether the request was successful
 * @param response The AI-generated response text
 * @param role The role of the responder (always "assistant")
 */
data class ChatResponse(
    val success: Boolean,
    val response: String,
    val role: String
)
