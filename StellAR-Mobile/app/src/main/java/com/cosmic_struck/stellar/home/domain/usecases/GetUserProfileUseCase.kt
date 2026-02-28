package com.cosmic_struck.stellar.home.domain.usecases

import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.UserProfile
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(private val databases: Databases) {
    operator fun invoke(userId: String): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading())
        try {
            val doc = databases.getDocument(
                databaseId = DATABASE_ID,
                collectionId = "users",
                documentId = userId
            )
            val profile = UserProfile(
                id = doc.id,
                created_at = doc.data["\$createdAt"] as? String ?: "",
                user_name = doc.data["user_name"] as? String ?: "User",
                level = (doc.data["level"] as? Number)?.toLong() ?: 1L,
                total_xp = (doc.data["total_xp"] as? Number)?.toDouble() ?: 0.0,
                user_pp = doc.data["user_pp"] as? String ?: ""
            )
            emit(Resource.Success(profile))
        } catch (e: Exception) {
            emit(Resource.Error("Could not load profile"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}