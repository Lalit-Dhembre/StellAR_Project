package com.cosmic_struck.stellar.classroom.domain.usecase

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomMember
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetClassroomMembersUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(classId: String): Flow<Resource<List<ClassroomMember>>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "get_classroom_members",
                parameters = mapOf("p_classroom_id" to classId)
            ).decodeList<ClassroomMember>()

            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load members"))
        }
    }
}