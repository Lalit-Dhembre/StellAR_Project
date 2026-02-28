package com.cosmic_struck.stellar.auth.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.ID
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import io.appwrite.models.InputFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val account: Account,
    private val databases: Databases,
    private val storage: Storage
) {
    operator fun invoke(
        username: String,
        email: String,
        password: String,
        imageUri: Uri?,
        context: Context
    ): Flow<Resource<Boolean>> =
        flow {
            try {
                emit(Resource.Loading())
                val user = account.create(
                    userId = ID.unique(),
                    email = email,
                    password = password,
                    name = username
                )
                Log.d("SignUpUseCase", user.toString())

                val userId = user.id

                // Create session after signup (may already exist if create() auto-logged in)
                try {
                    account.createEmailPasswordSession(
                        email = email,
                        password = password
                    )
                } catch (_: Exception) {
                    // Session already active from account.create() — safe to ignore
                }

                // Update user_name in users collection
                databases.updateDocument(
                    databaseId = DATABASE_ID,
                    collectionId = "users",
                    documentId = userId,
                    data = mapOf("user_name" to username)
                )

                if (imageUri != null) {
                    val imageUrl = uploadProfileImage(
                        context = context,
                        imageUri = imageUri,
                        userId = userId
                    )
                    databases.updateDocument(
                        databaseId = DATABASE_ID,
                        collectionId = "users",
                        documentId = userId,
                        data = mapOf("user_pp" to imageUrl)
                    )
                }
                Log.d("SignUpUseCase", "User Created")
                emit(Resource.Success(true))
            } catch (e: Exception) {
                emit(Resource.Error(e.localizedMessage ?: "Unknown Error"))
                Log.d("SignUpUseCase", e.localizedMessage ?: "Unknown Error")
            }
        }

    private suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): String {
        val bytes = context.contentResolver
            .openInputStream(imageUri)
            ?.readBytes()
            ?: throw Exception("Failed to read image")

        val fileId = ID.unique()
        val tempFile = java.io.File(context.cacheDir, "avatar_$userId.jpg")
        tempFile.writeBytes(bytes)

        storage.createFile(
            bucketId = "profile-pictures",
            fileId = fileId,
            file = InputFile.fromFile(tempFile)
        )

        tempFile.delete()

        // Construct the public URL
        val endpoint = com.cosmic_struck.stellar.BuildConfig.APPWRITE_ENDPOINT
        val projectId = com.cosmic_struck.stellar.BuildConfig.APPWRITE_PROJECT_ID
        return "$endpoint/storage/buckets/profile-pictures/files/$fileId/view?project=$projectId"
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}
