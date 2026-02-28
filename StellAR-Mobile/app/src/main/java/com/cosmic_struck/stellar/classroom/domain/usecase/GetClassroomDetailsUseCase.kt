package com.cosmic_struck.stellar.classroom.domain.usecase

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomDetail
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetClassroomDetailsUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(classId: String): Flow<Resource<ClassroomDetail>> = flow {
        emit(Resource.Loading())
        try {
            val doc = databases.getDocument(
                databaseId = DATABASE_ID,
                collectionId = "classroom",
                documentId = classId
            )

            // Get creator name
            val creatorId = doc.data["created_by"] as? String ?: ""
            var creatorName = ""
            if (creatorId.isNotEmpty()) {
                try {
                    val creatorDoc = databases.getDocument(
                        databaseId = DATABASE_ID,
                        collectionId = "users",
                        documentId = creatorId
                    )
                    creatorName = creatorDoc.data["user_name"] as? String ?: ""
                } catch (_: Exception) {}
            }

            // Count members
            val membersDocs = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "classroom_members",
                queries = listOf(Query.equal("classroom_id", classId))
            )

            val detail = ClassroomDetail(
                name = doc.data["name"] as? String ?: "",
                creator_name = creatorName,
                member_count = membersDocs.total,
                creator_id = creatorId,
                classroom_code = doc.data["join_code"] as? String ?: ""
            )
            emit(Resource.Success(detail))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load classroom details"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}