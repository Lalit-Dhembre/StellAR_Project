package com.cosmic_struck.stellar.classroom.data.local.repository

import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import com.cosmic_struck.stellar.classroom.data.dto.Quiz
import com.cosmic_struck.stellar.classroom.data.local.dao.ModuleProcessDao
import com.cosmic_struck.stellar.classroom.data.local.entity.ModuleProcessEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for local database operations on module process data
 */
@Singleton
class ModuleProcessLocalRepository @Inject constructor(
    private val dao: ModuleProcessDao
) {
    private val gson = Gson()

    /**
     * Get cached ProcessResponse for a module
     */
    suspend fun getProcessResponse(moduleId: Long): ProcessResponse? {
        val entity = dao.getByModuleId(moduleId) ?: return null
        val quizList = parseQuizList(entity.quizJson)
        return ProcessResponse(
            quiz = quizList,
            quiz_count = entity.quizCount,
            success = true,
            summary = entity.summary,
            extracted_text = entity.extractedText
        )
    }

    /**
     * Save ProcessResponse to database
     */
    suspend fun saveProcessResponse(moduleId: Long, response: ProcessResponse) {
        val entity = ModuleProcessEntity(
            moduleId = moduleId,
            summary = response.summary,
            quizJson = gson.toJson(response.quiz),
            quizCount = response.quiz_count,
            extractedText = response.extracted_text
        )
        dao.insert(entity)
    }

    /**
     * Check if data exists for a module
     */
    suspend fun hasData(moduleId: Long): Boolean {
        return dao.exists(moduleId)
    }

    /**
     * Delete cached data for a module
     */
    suspend fun deleteData(moduleId: Long) {
        dao.deleteByModuleId(moduleId)
    }

    /**
     * Clear all cached data
     */
    suspend fun clearAll() {
        dao.deleteAll()
    }

    private fun parseQuizList(json: String): List<Quiz> {
        return try {
            val type = object : TypeToken<List<Quiz>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
