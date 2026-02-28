package com.cosmic_struck.stellar.home.domain.usecases

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateUserProfilePictureUseCase @Inject constructor(
    private val databases: Databases,
    private val storage: Storage
) {
    operator fun invoke(
        userId: String,
        imageUri: Uri,
        context: Context
    ): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                ?: throw Exception("Failed to read image")

            val fileId = ID.unique()
            val tempFile = java.io.File(context.cacheDir, "avatar_$userId.jpg")
            tempFile.writeBytes(bytes)

            // Upload (new file each time; old file will remain in storage)
            storage.createFile(
                bucketId = "profile-pictures",
                fileId = fileId,
                file = InputFile.fromFile(tempFile)
            )

            tempFile.delete()

            // Get Public URL
            val endpoint = com.cosmic_struck.stellar.BuildConfig.APPWRITE_ENDPOINT
            val projectId = com.cosmic_struck.stellar.BuildConfig.APPWRITE_PROJECT_ID
            val publicUrl = "$endpoint/storage/buckets/profile-pictures/files/$fileId/view?project=$projectId"

            // Update users collection
            databases.updateDocument(
                databaseId = DATABASE_ID,
                collectionId = "users",
                documentId = userId,
                data = mapOf("user_pp" to publicUrl)
            )

            Log.d("UpdateProfilePic", "Success: $publicUrl")
            emit(Resource.Success(publicUrl))

        } catch (e: Exception) {
            Log.e("UpdateProfilePic", "Error", e)
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}
