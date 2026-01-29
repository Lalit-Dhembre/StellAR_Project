package com.cosmic_struck.stellar.auth.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient
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
                val user = supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Log.d("SignUpUseCase", user.toString())
                val userId = user?.id
                if (userId != null) {
                    supabaseClient.postgrest.from("users").update({
                        set("user_name", username)
                    }) {
                        filter {
                            eq("id", userId)
                        }
                    }
                    if (imageUri != null) {
                        val imageUrl = uploadProfileImage(
                            supabaseClient = supabaseClient,
                            context = context,
                            imageUri = imageUri,
                            userId = userId
                        )
                        supabaseClient.postgrest.from("users").update({
                            set("user_pp", imageUrl)
                        }) {
                            filter {
                                eq("id", userId)
                            }
                        }
                    }
                    Log.d("SignUpUseCase", "User Created")
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error("Could not create user"))
                }
            } catch (e: Exception) {
                emit(Resource.Error(e.localizedMessage ?: "Unknown Error"))
                Log.d("SignUpUseCase", e.localizedMessage ?: "Unknown Error")
            }
        }
}

private suspend fun uploadProfileImage(
    supabaseClient: SupabaseClient,
    context: Context,
    imageUri: Uri,
    userId: String
): String {
    val bytes = context.contentResolver
        .openInputStream(imageUri)
        ?.readBytes()
        ?: throw Exception("Failed to read image")

    val fileName = "avatars/$userId.jpg"

    supabaseClient.storage
        .from("profile_pictures")
        .upload(
            path = fileName,
            data = bytes,
            options = {
                upsert = true
            }
        )

    return supabaseClient.storage
        .from("profile_pictures")
        .publicUrl(fileName)
}
