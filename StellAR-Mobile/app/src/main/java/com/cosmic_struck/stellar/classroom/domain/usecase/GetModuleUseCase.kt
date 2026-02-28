package com.cosmic_struck.stellar.classroom.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule1
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetModuleUseCase @Inject constructor(
    private val databases: Databases
) {
    operator fun invoke(id: Long) : Flow<Resource<ClassroomModule1>> = flow{
        emit(Resource.Loading())
        try{
            val result = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "modules",
                queries = listOf(Query.equal("id", id))
            )

            if (result.documents.isEmpty()) {
                emit(Resource.Error("Module not found"))
                return@flow
            }

            val doc = result.documents[0]
            val module = ClassroomModule1(
                id = (doc.data["id"] as? Number)?.toLong() ?: id,
                moduleName = doc.data["module_name"] as? String,
                moduleDesc = doc.data["module_desc"] as? String,
                imageUrl = doc.data["image_url"] as? String,
                modelUrl = doc.data["model_url"] as? String,
                pdfUrl = doc.data["pdf_url"] as? String,
                classroomId = doc.data["classroom_id"] as? String
            )

            Log.d("CHECKING","MODULES = ${module.toString()}")
            emit(Resource.Success(module))
        } catch (e: Exception){
            emit(Resource.Error(e.localizedMessage ?: "Failed to load module"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}