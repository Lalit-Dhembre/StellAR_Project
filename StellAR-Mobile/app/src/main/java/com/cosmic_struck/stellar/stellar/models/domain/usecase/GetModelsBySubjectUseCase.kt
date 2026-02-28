package com.cosmic_struck.stellar.stellar.models.domain.usecase

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetModelsBySubjectUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(subject: String): Flow<Resource<List<ClassroomModel>>> = flow {
        emit(Resource.Loading())
        try {
            val result = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "models",
                queries = listOf(Query.equal("model_subject", subject))
            )

            val models = result.documents.map { doc ->
                ClassroomModel(
                    model_id = doc.data["model_id"] as? String ?: doc.id,
                    model_name = doc.data["model_name"] as? String ?: "",
                    description = doc.data["description"] as? String,
                    model_url = doc.data["model_url"] as? String ?: "",
                    model_thumbnail = doc.data["model_thumbnail"] as? String,
                    rarity = doc.data["rarity"] as? String ?: "",
                    xp_reward = (doc.data["xp_reward"] as? Number)?.toInt(),
                    model_subject = doc.data["model_subject"] as? String ?: subject,
                    min_level = (doc.data["min_level"] as? Number)?.toInt() ?: 0,
                    created_at = doc.data["\$createdAt"] as? String ?: ""
                )
            }

            emit(Resource.Success(models))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load models for $subject"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}