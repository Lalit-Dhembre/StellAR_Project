package com.cosmic_struck.stellar.home.domain.usecases

import androidx.media3.common.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.CreateClassroomResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CreateClassroomUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(userId: String, classroomName: String): Flow<Resource<CreateClassroomResponse>> = flow {
        emit(Resource.Loading())
        try {
            Log.d("Checking Values","$userId, $classroomName")
            val response = client.postgrest.rpc(
                function = "create_classroom",
                parameters = mapOf("p_user_id" to userId, "p_classroom_name" to classroomName)
            ).decodeAs<CreateClassroomResponse>()

            if (response.status == "success") {
                emit(Resource.Success(response))
                Log.d("CREATE CLASSROOM USECASE", response.toString())
            } else {
                emit(Resource.Error(response.message))
                Log.d("CREATE CLASSROOM USECASE", response.message)
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
            Log.d("CREATE CLASSROOM USECASE", e.localizedMessage ?: "Unknown error")
        }
    }
}
