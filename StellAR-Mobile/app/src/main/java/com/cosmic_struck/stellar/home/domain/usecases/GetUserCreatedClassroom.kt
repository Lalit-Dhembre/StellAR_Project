package com.cosmic_struck.stellar.home.domain.usecases

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.JoinedClassroom
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserCreatedClassroom @Inject constructor(
    private val databases: Databases
){
    operator fun invoke(userId: String) : Flow<Resource<List<JoinedClassroom>>> = flow {
        emit(Resource.Loading())
        try {
            val result = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "classroom",
                queries = listOf(Query.equal("created_by", userId))
            )

            val classrooms = result.documents.map { doc ->
                // Count members for each classroom
                val membersDocs = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = "classroom_members",
                    queries = listOf(Query.equal("classroom_id", doc.id))
                )

                // Get creator name
                var creatorName = ""
                try {
                    val creatorDoc = databases.getDocument(
                        databaseId = DATABASE_ID,
                        collectionId = "users",
                        documentId = userId
                    )
                    creatorName = creatorDoc.data["user_name"] as? String ?: ""
                } catch (_: Exception) {}

                JoinedClassroom(
                    classroom_id = doc.id,
                    classroom_name = doc.data["name"] as? String ?: "",
                    join_code = doc.data["join_code"] as? String ?: "",
                    created_at = doc.data["\$createdAt"] as? String ?: "",
                    creator_name = creatorName,
                    member_count = membersDocs.total,
                    is_creator = true
                )
            }

            emit(Resource.Success(classrooms))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
            Log.d("Error", e.localizedMessage.toString())
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}