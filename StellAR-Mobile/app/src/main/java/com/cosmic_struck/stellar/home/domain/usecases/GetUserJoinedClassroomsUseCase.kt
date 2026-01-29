package com.cosmic_struck.stellar.home.domain.usecases

import android.util.Log
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.JoinedClassroom
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserJoinedClassroomsUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(userId: String): Flow<Resource<List<JoinedClassroom>>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "get_user_classrooms_v3",
                parameters = mapOf("p_user_id" to userId)
            ).decodeList<JoinedClassroom>()
            Log.d("GET USER JOINED CLASSROOMS",response.toString())
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
            Log.d("GET USER JOINED CLASSROOMS",e.localizedMessage.toString())
        }
    }
}