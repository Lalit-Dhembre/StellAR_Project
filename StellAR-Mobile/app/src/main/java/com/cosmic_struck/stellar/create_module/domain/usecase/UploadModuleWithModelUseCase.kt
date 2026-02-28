package com.cosmic_struck.stellar.create_module.domain.usecase

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

class UploadModuleWithModelUseCase @Inject constructor(
    private val databases: Databases,
    private val storage: Storage
) {
    operator fun invoke(
        moduleName: String,
        description: String,
        classroomId: String,
        imageUri: Uri?,
        context: Context,
        modelUri: Uri,
        pdfUri: Uri
    ) : Flow<Resource<Boolean>> =
        flow {
            emit(Resource.Loading())
            try {
                Log.d("Checking Inputs","moduleName: $moduleName, description: $description, classroomId: $classroomId, imageUri: $imageUri, modelUri: $modelUri, pdfUri: $pdfUri")

                val pdfUrl = uploadFile(context, pdfUri, "pdfs/$classroomId", ".pdf")
                var imageUrl = ""
                if (imageUri != null){
                    imageUrl = uploadFile(context, imageUri, "modules/$classroomId", ".jpg")
                }

                val modelUrl = uploadFile(context, modelUri, "models/$classroomId", ".glb")

                databases.createDocument(
                    databaseId = DATABASE_ID,
                    collectionId = "modules",
                    documentId = ID.unique(),
                    data = mapOf(
                        "module_name" to moduleName,
                        "module_desc" to description,
                        "classroom_id" to classroomId,
                        "image_url" to imageUrl,
                        "model_url" to modelUrl,
                        "pdf_url" to pdfUrl
                    )
                )

                emit(Resource.Success(true))
            } catch(e: Exception) {
                emit(Resource.Error(e.message.toString()))
                Log.d("Error Message", e.message.toString())
            }
        }

    private suspend fun uploadFile(
        context: Context,
        uri: Uri,
        pathPrefix: String,
        extension: String
    ): String {
        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.readBytes()
            ?: throw Exception("File read failed")

        val fileId = ID.unique()
        val tempFile = java.io.File(context.cacheDir, "${fileId}${extension}")
        tempFile.writeBytes(bytes)

        storage.createFile(
            bucketId = "user-uploads",
            fileId = fileId,
            file = InputFile.fromFile(tempFile)
        )

        tempFile.delete()

        val endpoint = com.cosmic_struck.stellar.BuildConfig.APPWRITE_ENDPOINT
        val projectId = com.cosmic_struck.stellar.BuildConfig.APPWRITE_PROJECT_ID
        return "$endpoint/storage/buckets/user-uploads/files/$fileId/view?project=$projectId"
    }

    companion object {
        const val DATABASE_ID = "stellar_db"
    }
}