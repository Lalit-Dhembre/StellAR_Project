package com.cosmic_struck.stellar.classroom.domain.model

data class QuizResult(
    val correctAnswers: Int,
    val totalQuestions: Int,
    val accuracy: Int,
    val timeSeconds: Long,
    val xpEarned: Int
)