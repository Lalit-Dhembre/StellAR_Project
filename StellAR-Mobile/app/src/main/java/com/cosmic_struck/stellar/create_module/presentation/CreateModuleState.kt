package com.cosmic_struck.stellar.create_module.presentation

import android.net.Uri

data class CreateModuleState(
    val moduleName : String = "",
    val classroom_id : String = "",
    val isLoading : Boolean = false,
    val error: String? = null,
    val imagePath: Uri? = null,
    val pdfPath: Uri? = null,
    val modelPath: Uri? = null,
    val description : String = "",
    val modelChoice: ModelChoice = ModelChoice.UPLOAD_MODEL,
    val selectedOption: Int = 0,

    val uploadSuccess : UploadStatus = UploadStatus.IDLE,
    val generationStatus: UploadStatus = UploadStatus.IDLE
)

enum class UploadStatus{
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}


enum class ModelChoice{
    UPLOAD_MODEL,
    GENERATE_MODEL
}
