package com.cosmic_struck.stellar.classroom.domain.usecase

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomMember
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetClassroomMembersUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(classId: String): Flow<Resource<List<ClassroomMember>>> = flow {
        emit(Resource.Loading())
        try {
            val memberships = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "classroom_members",
                queries = listOf(Query.equal("classroom_id", classId))
            )

            val members = mutableListOf<ClassroomMember>()
            for (membership in memberships.documents) {
                val userId = membership.data["user_id"] as? String ?: continue
                val joinedAt = membership.data["\$createdAt"] as? String ?: ""
                try {
                    val userDoc = databases.getDocument(
                        databaseId = DATABASE_ID,
                        collectionId = "users",
                        documentId = userId
                    )
                    members.add(
                        ClassroomMember(
                            user_id = userId,
                            user_name = userDoc.data["user_name"] as? String ?: "",
                            level = (userDoc.data["level"] as? Number)?.toLong() ?: 1L,
                            total_xp = (userDoc.data["total_xp"] as? Number)?.toDouble() ?: 0.0,
                            joined_at = joinedAt
                        )
                    )
                } catch (_: Exception) {}
            }

            emit(Resource.Success(members))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load members"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}