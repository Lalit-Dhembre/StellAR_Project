package com.cosmic_struck.stellar.classroom.domain.usecase

import android.util.Log
import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import com.cosmic_struck.stellar.classroom.data.repository.ClassroomModuleServiceRepository
import com.cosmic_struck.stellar.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import javax.inject.Inject

class GetProcessResponseUseCase @Inject constructor(
    private val classroomModuleServiceRepository: ClassroomModuleServiceRepository
) {
    operator fun invoke(description: String, pdf: MultipartBody.Part) : Flow<Resource<ProcessResponse>> = flow {
        emit(Resource.Loading())
        try {
            val info = classroomModuleServiceRepository.getProcessResponse(description, pdf)
            emit(Resource.Success(info))
            Log.d("Process Info","$info")
        }catch (e: Exception){
            emit(Resource.Error(e.message ?: "Unexpected Error"))
            Log.d("Process Info","${e.message}")
        }
    }
}