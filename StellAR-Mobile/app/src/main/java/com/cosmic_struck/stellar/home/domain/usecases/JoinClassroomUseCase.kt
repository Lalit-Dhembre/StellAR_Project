package com.cosmic_struck.stellar.home.domain.usecases

import androidx.media3.common.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.JoinResponse
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class JoinClassroomUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(userId: String, joinCode: String): Flow<Resource<JoinResponse>> = flow {
        emit(Resource.Loading())
        try {
            // Find classroom by join_code
            val classrooms = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "classroom",
                queries = listOf(Query.equal("join_code", joinCode))
            )

            if (classrooms.documents.isEmpty()) {
                emit(Resource.Error("No classroom found with that code"))
                return@flow
            }

            val classroomId = classrooms.documents[0].id

            // Check if already a member
            val existing = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = "classroom_members",
                queries = listOf(
                    Query.equal("classroom_id", classroomId),
                    Query.equal("user_id", userId)
                )
            )

            if (existing.documents.isNotEmpty()) {
                emit(Resource.Error("Already a member of this classroom"))
                return@flow
            }

            // Add as member
            databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = "classroom_members",
                documentId = ID.unique(),
                data = mapOf(
                    "classroom_id" to classroomId,
                    "user_id" to userId
                )
            )

            val response = JoinResponse(
                status = "success",
                message = "Successfully joined classroom"
            )
            emit(Resource.Success(response))
            Log.d("JOIN CLASSROOM USECASE", response.toString())
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
            Log.d("JOIN CLASSROOM USECASE", e.localizedMessage.toString())
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}