package com.cosmic_struck.stellar.classroom.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject

class GetClassroomModelsUseCase @Inject constructor(private val client: SupabaseClient) {
    operator fun invoke(classId: String): Flow<Resource<List<ClassroomModule>>> = flow {
        emit(Resource.Loading())
        try {
            val response = client.postgrest.rpc(
                function = "get_classroom_modules_v3",
                parameters = mapOf("p_classroom_id" to classId)
            ).decodeList<ClassroomModule>()
            Log.d("CHECKING","MODELS = ${response.toString()}")
            emit(Resource.Success(response))
        } catch (e: Exception) {
            Log.d("CHECKING","${e.localizedMessage.toString()}")
            emit(Resource.Error(e.localizedMessage ?: "Failed to load models for this classroom"))
        }
    }
}