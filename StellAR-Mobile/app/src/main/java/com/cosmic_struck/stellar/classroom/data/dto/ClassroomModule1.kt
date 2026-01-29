package com.cosmic_struck.stellar.classroom.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClassroomModule1(
     val id: Long,
    @SerialName("module_name") val moduleName: String?,
    @SerialName("module_desc") val moduleDesc: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("model_url") val modelUrl: String?,
    @SerialName("pdf_url") val pdfUrl: String?,
    @SerialName("classroom_id") val classroomId: String? // UUIDs are strings in JSON
)