package com.cosmic_struck.stellar.stellar.models.domain.usecase

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject

class GetModelsBySubjectUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(subject: String): Flow<Resource<List<ClassroomModel>>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "get_models_by_subject",
                parameters = mapOf("p_subject" to subject)
            ).decodeList<ClassroomModel>()

            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load models for $subject"))
        }
    }
}