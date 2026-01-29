package com.cosmic_struck.stellar.classroom.domain.usecase

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomDetail
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetClassroomDetailsUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(classId: String): Flow<Resource<ClassroomDetail>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "get_classroom_details_v3",
                parameters = mapOf("p_classroom_id" to classId)
            ).decodeSingle<ClassroomDetail>()

            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load classroom details"))
        }
    }
}