package com.cosmic_struck.stellar.create_module.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.UploadOptionBuilder
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UploadModuleWithModelUseCase @Inject constructor(
    private val supabase: SupabaseClient
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

                val pdfUrl = uploadPdf(
                    context, pdfUri, classroomId
                )
                var imageUrl = ""
                if (imageUri != null){
                    imageUrl = uploadModuleImage(
                        context, imageUri, classroomId
                    )
                }


                val modelUrl = uploadModuleModel(
                     context, modelUri, classroomId
                )
                supabase.postgrest.rpc(
                    function = "create_classroom_module_v4",
                    parameters = mapOf(
                        "p_module_name" to moduleName,
                        "p_module_desc" to description,
                        "p_classroom_id" to classroomId,
                        "p_image_url" to imageUrl,
                        "p_model_url" to modelUrl,
                        "p_pdf_url" to pdfUrl
                    )
                )
                emit(Resource.Success(true))
            }catch (e: Exception){
                emit(Resource.Error(e.message.toString()))
                Log.d("Error Message",e.message.toString())
            }
        }


suspend fun uploadPdf(
    context: Context,
    uri: Uri,
    classroomId: String
): String{
    val bytes = context
        .contentResolver
        .openInputStream(uri)
        ?.readBytes()
        ?: throw Exception("PDF read failed")

    val path = "pdfs/$classroomId/${System.currentTimeMillis()}.pdf"
    supabase.storage
        .from("user_uploads")
        .upload(
            path = path,
            data = bytes,
            options = {
                upsert = true
            }
        )
    return supabase.storage
        .from("user_uploads")
        .publicUrl(path)
}
 suspend fun uploadModuleModel(
        context: Context,
        uri: Uri,
        classroomId: String
    ): String {

        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.readBytes()
            ?: throw Exception("Model read failed")

        val path = "models/$classroomId/${System.currentTimeMillis()}.glb"

        supabase.storage
            .from("user_uploads")
            .upload(
                path = path,
                data = bytes,
                options = {
                    upsert = true
                }
            )

        return supabase.storage
            .from("user_uploads")
            .publicUrl(path)
    }

    suspend fun uploadModuleImage(
        context: Context,
        uri: Uri,
        classroomId: String
    ): String {

        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.readBytes()
            ?: throw Exception("Image read failed")

        val path = "modules/$classroomId/${System.currentTimeMillis()}.jpg"

        supabase.storage
            .from("user_uploads")
            .upload(
                path = path,
                data = bytes,
                options = {
                    upsert = true
                }
            )

        return supabase.storage
            .from("user_uploads")
            .publicUrl(path)
    }


}