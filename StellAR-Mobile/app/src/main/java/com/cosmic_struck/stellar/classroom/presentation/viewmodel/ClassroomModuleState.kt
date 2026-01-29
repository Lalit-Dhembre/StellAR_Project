package com.cosmic_struck.stellar.classroom.presentation.viewmodel

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule1
import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse

data class ClassroomModuleState(
    val module_id: Long? = null,
    val module: ClassroomModule1? = null,
    val model_path : String = "",
    val pdf_path : String = "",
    val isLoading : Boolean = false,
    val moduleError : String = "",
    val processError : String = "",
    val processInfo: ProcessResponse? = null,
)
