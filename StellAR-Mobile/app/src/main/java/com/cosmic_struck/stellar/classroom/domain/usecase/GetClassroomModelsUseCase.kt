package com.cosmic_struck.stellar.classroom.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetClassroomModelsUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(classId: String): Flow<Resource<List<ClassroomModule>>> = flow {
        emit(Resource.Loading())
        try {
            val result = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "modules",
                queries = listOf(Query.equal("classroom_id", classId))
            )

            val modules = result.documents.map { doc ->
                ClassroomModule(
                    id = (doc.data["module_id"] as? Number)?.toLong() ?: doc.id.hashCode().toLong(),
                    moduleName = doc.data["module_name"] as? String,
                    moduleDesc = doc.data["module_desc"] as? String,
                    imageUrl = doc.data["image_url"] as? String,
                    modelUrl = doc.data["model_url"] as? String,
                    pdfUrl = doc.data["pdf_url"] as? String,
                    classroomId = doc.data["classroom_id"] as? String
                )
            }

            Log.d("CHECKING","MODELS = ${modules.toString()}")
            emit(Resource.Success(modules))
        } catch (e: Exception) {
            Log.d("CHECKING","${e.localizedMessage.toString()}")
            emit(Resource.Error(e.localizedMessage ?: "Failed to load models for this classroom"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}