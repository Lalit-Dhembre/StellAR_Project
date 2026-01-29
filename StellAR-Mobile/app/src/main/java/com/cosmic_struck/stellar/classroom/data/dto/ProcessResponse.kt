package com.cosmic_struck.stellar.classroom.data.dto

data class ProcessResponse(
    val quiz: List<Quiz>,
    val quiz_count: Int,
    val success: Boolean,
    val summary: String,
    val extracted_text: String? = null
)