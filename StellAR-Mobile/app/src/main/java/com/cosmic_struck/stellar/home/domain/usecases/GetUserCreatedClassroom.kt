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

class GetUserCreatedClassroom @Inject constructor(
    private val supabaseClient: SupabaseClient
){
    operator fun invoke(userId: String) : Flow<Resource<List<JoinedClassroom>>> = flow {
        emit(Resource.Loading())
        try {
            val classrooms = supabaseClient
                .postgrest
                .rpc(
                    function = "get_classrooms_created_by_user",
                    parameters = mapOf("p_user_id" to userId)
                )
                .decodeList<JoinedClassroom>()
            emit(Resource.Success(classrooms))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
            Log.d("Error",e.localizedMessage.toString())
        }
    }
}