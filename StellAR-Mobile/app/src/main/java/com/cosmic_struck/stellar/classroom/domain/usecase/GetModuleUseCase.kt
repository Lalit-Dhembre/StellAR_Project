package com.cosmic_struck.stellar.classroom.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule1
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetModuleUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    operator fun invoke(id:Long) : Flow<Resource<ClassroomModule1>> = flow{
        emit(Resource.Loading())
        try{
            val modules = supabaseClient.postgrest.rpc(
                function = "get_module_details",
                parameters = mapOf("p_module_id" to id)
            ).decodeList<ClassroomModule1>()
            Log.d("CHECKING","MODULES = ${modules[0].toString()}")
            emit(Resource.Success(modules[0]))
        }catch (e: Exception){
            emit(Resource.Error(e.localizedMessage ?: "Failed to load module"))
        }
    }
}