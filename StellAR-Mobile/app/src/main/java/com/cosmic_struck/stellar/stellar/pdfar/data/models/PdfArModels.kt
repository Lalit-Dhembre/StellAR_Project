package com.cosmic_struck.stellar.stellar.pdfar.data.models

import com.google.gson.annotations.SerializedName

// Models for POST /api/rag/process-content
data class ProcessPdfResponse(
    val success: Boolean,
    val concepts: List<Concept>? = null,
    @SerializedName("native_images") val nativeImages: List<NativeImage>? = null,
    val error: String? = null,
    val details: Any? = null
)

data class Concept(
    val id: String,
    val title: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_caption") val imageCaption: String?,
    val script: String?,
    val score: Float?,
    val source: String?
)

data class NativeImage(
    val id: String,
    val title: String,
    @SerializedName("image_url") val imageUrl: String,
    val page: Int,
    val source: String
)

// Models for POST /api/rag/concept-details
data class ConceptDetailsRequest(
    @SerializedName("concept_id") val conceptId: String
)

data class ConceptDetailsResponse(
    val title: String,
    @SerializedName("image_url") val imageUrl: String?,
    val script: String?,
    @SerializedName("model_url") val modelUrl: String?,
    @SerializedName("model_status") val modelStatus: String
)

// Legacy endpoints falling back from resolveEntityAndPoll
data class ResolveEntityRequest(
    val entity: String
)

data class ResolveEntityResponse(
    val status: String,
    @SerializedName("model_url") val modelUrl: String? = null,
    @SerializedName("task_id") val taskId: String? = null
)

data class TaskStatusResponse(
    val status: String,
    @SerializedName("model_url") val modelUrl: String? = null
)
