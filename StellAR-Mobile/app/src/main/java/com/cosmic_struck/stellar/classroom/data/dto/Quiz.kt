package com.cosmic_struck.stellar.classroom.data.dto

data class Quiz(
    val correct_answer: String,
    val options: List<String>,
    val question: String
)