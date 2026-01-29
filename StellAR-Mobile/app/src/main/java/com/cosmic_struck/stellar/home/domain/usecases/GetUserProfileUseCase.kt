package com.cosmic_struck.stellar.home.domain.usecases

import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.data.dto.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(userId: String): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "get_user_by_id_v2",
                parameters = mapOf("p_user_id" to userId)
            ).decodeSingle<UserProfile>()
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("Could not load profile"))
        }
    }
}