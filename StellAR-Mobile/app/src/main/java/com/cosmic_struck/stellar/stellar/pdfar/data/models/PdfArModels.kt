package com.cosmic_struck.stellar.stellar.pdfar.data.models

import com.google.gson.annotations.SerializedName

data class Section(
    val id: String,
    val title: String,
    val entities: List<String>,
    val imageUrl: String? = null
)

data class ProcessPdfResponse(
    val success: Boolean,
    val domain_match: Boolean,
    val message: String? = null,
    val documents: List<Document>? = null,
    val max_sections: Int? = null
)

data class Document(
    val page_content: String,
    val metadata: Metadata? = null
)

data class Metadata(
    val title: String? = null,
    val entity: String? = null,
    val image_url: String? = null
)

data class ResolveEntityRequest(
    val entity: String
)

data class ResolveEntityResponse(
    val status: String, // "ready" or "processing"
    @SerializedName("model_url") val modelUrl: String? = null,
    @SerializedName("task_id") val taskId: String? = null
)

data class TaskStatusResponse(
    val status: String, // "completed" or "processing"
    @SerializedName("model_url") val modelUrl: String? = null
)
