package com.cosmic_struck.stellar.classroom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cosmic_struck.stellar.classroom.data.local.converter.QuizListConverter

/**
 * Room entity for storing ProcessResponse data (quiz, summary, and extracted text)
 * with module_id as primary key for efficient lookup
 */
@Entity(tableName = "module_process_data")
@TypeConverters(QuizListConverter::class)
data class ModuleProcessEntity(
    @PrimaryKey
    val moduleId: Long,
    val summary: String,
    val quizJson: String, // JSON string of quiz list
    val quizCount: Int,
    val extractedText: String? = null, // Document text for chatbot context
    val createdAt: Long = System.currentTimeMillis()
)

