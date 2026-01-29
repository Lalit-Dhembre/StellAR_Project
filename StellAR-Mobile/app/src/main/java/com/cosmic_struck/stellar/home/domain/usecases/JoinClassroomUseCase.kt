package com.cosmic_struck.stellar.home.domain.usecases

import androidx.media3.common.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.JoinResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class JoinClassroomUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(userId: String, joinCode: String): Flow<Resource<JoinResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "join_classroom_by_code",
                parameters = mapOf("p_user_id" to userId, "p_join_code" to joinCode)
            ).decodeAs<JoinResponse>()

            if (response.status == "success") {
                emit(Resource.Success(response))
                Log.d("JOIN CLASSROOM USECASE",response.toString())
            } else {
                emit(Resource.Error(response.message))
                Log.d("JOIN CLASSROOM USECASE",response.message.toString())
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
            Log.d("JOIN CLASSROOM USECASE",e.localizedMessage.toString())
        }
    }
}