package com.cosmic_struck.stellar.home.domain.usecases

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateUserProfilePictureUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient
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

            val fileName = "avatars/$userId.jpg"

            // Upload (upsert=true overwrites existing)
            supabaseClient.storage.from("profile_pictures").upload(
                path = fileName,
                data = bytes,
                options = {
                    upsert = true
                }
            )

            // Get Public URL
            // Add timestamp query param to bypass CDN caching if needed, though for DB storage we usually want clean URL.
            // But if we want the app to update immediately, we might need to handle caching in the UI.
            val publicUrl = supabaseClient.storage.from("profile_pictures").publicUrl(fileName)
            
            // Update users table
            supabaseClient.postgrest.from("users").update({
                set("user_pp", publicUrl)
            }) {
                filter {
                    eq("id", userId)
                }
            }

            Log.d("UpdateProfilePic", "Success: $publicUrl")
            emit(Resource.Success(publicUrl))

        } catch (e: Exception) {
            Log.e("UpdateProfilePic", "Error", e)
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
}
