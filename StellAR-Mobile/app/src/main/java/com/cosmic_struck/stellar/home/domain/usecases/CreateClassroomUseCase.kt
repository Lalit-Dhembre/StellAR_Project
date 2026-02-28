package com.cosmic_struck.stellar.home.domain.usecases

import androidx.media3.common.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.CreateClassroomResponse
import io.appwrite.ID
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CreateClassroomUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(userId: String, classroomName: String): Flow<Resource<CreateClassroomResponse>> = flow {
        emit(Resource.Loading())
        try {
            Log.d("Checking Values","$userId, $classroomName")

            // Generate a random join code
            val joinCode = (100000..999999).random().toString()

            // Create the classroom document
            val doc = databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = "classroom",
                documentId = ID.unique(),
                data = mapOf(
                    "name" to classroomName,
                    "created_by" to userId,
                    "join_code" to joinCode
                )
            )

            // Also add creator as a member
            databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = "classroom_members",
                documentId = ID.unique(),
                data = mapOf(
                    "classroom_id" to doc.id,
                    "user_id" to userId
                )
            )

            val response = CreateClassroomResponse(
                status = "success",
                message = "Classroom created successfully",
                classroom_id = doc.id,
                join_code = joinCode
            )
            emit(Resource.Success(response))
            Log.d("CREATE CLASSROOM USECASE", response.toString())
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
            Log.d("CREATE CLASSROOM USECASE", e.localizedMessage ?: "Unknown error")
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}
