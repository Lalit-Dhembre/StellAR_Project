package com.cosmic_struck.stellar.home.domain.usecases

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.JoinedClassroom
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserJoinedClassroomsUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(userId: String): Flow<Resource<List<JoinedClassroom>>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Get all classroom_members entries for this user
            val memberships = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "classroom_members",
                queries = listOf(Query.equal("user_id", userId))
            )

            val classrooms = mutableListOf<JoinedClassroom>()

            for (membership in memberships.documents) {
                val classroomId = membership.data["classroom_id"] as? String ?: continue

                try {
                    val classroomDoc = databases.getDocument(
                        databaseId = DATABASE_ID,
                        collectionId = "classroom",
                        documentId = classroomId
                    )

                    // Get creator name
                    val creatorId = classroomDoc.data["created_by"] as? String ?: ""
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
                        queries = listOf(Query.equal("classroom_id", classroomId))
                    )

                    classrooms.add(
                        JoinedClassroom(
                            classroom_id = classroomId,
                            classroom_name = classroomDoc.data["name"] as? String ?: "",
                            join_code = classroomDoc.data["join_code"] as? String ?: "",
                            created_at = classroomDoc.data["\$createdAt"] as? String ?: "",
                            creator_name = creatorName,
                            member_count = membersDocs.total,
                            is_creator = creatorId == userId
                        )
                    )
                } catch (e: Exception) {
                    Log.d("GetUserJoinedClassrooms", "Skipping classroom $classroomId: ${e.message}")
                }
            }

            Log.d("GET USER JOINED CLASSROOMS", classrooms.toString())
            emit(Resource.Success(classrooms))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
            Log.d("GET USER JOINED CLASSROOMS", e.localizedMessage.toString())
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}