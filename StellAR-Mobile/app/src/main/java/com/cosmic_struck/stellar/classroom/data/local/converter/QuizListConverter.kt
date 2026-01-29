package com.cosmic_struck.stellar.classroom.data.local.converter

import androidx.room.TypeConverter
import com.cosmic_struck.stellar.classroom.data.dto.Quiz
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converter for storing List<Quiz> as JSON string in Room
 */
class QuizListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromQuizList(quizList: List<Quiz>?): String {
        return gson.toJson(quizList ?: emptyList<Quiz>())
    }

    @TypeConverter
    fun toQuizList(json: String?): List<Quiz> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Quiz>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
