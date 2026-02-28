package com.cosmic_struck.stellar.stellar.scantext.data.dto

/**
 * Response DTOs for the PDF Ingestion Pipeline.
 * Maps to the JSON response from POST /api/pdf/upload
 */

// ── Main Upload Response ──
data class PdfUploadDTO(
    val success: Boolean,
    val rejected: Boolean? = null,
    val reason: String? = null,
    val job_id: String? = null,
    val document: DocumentDTO? = null,
    val concepts: List<ConceptDTO>? = null,
    val validation: ValidationDTO? = null,
    val error: String? = null
)

// ── Document Structure ──
data class DocumentDTO(
    val title: String?,
    val page_count: Int,
    val domain: String?,
    val domain_confidence: Double?,
    val sections: List<SectionDTO>?,
    val figures: List<FigureDTO>?,
    val raw_text: String?
)

data class SectionDTO(
    val type: String,
    val level: Int?,
    val text: String,
    val page: Int
)

data class FigureDTO(
    val id: String,
    val caption: String?,
    val image_url: String?,
    val page: Int?
)

// ── Concept Chunks (Phase 3) ──
data class ConceptDTO(
    val concept_title: String,
    val content_text: String,
    val keywords: List<String>?,
    val related_figures: List<RelatedFigureDTO>?,
    val page_range: List<Int>?,
    val assets: List<AssetDTO>?,
    val asset_source: String?
)

data class RelatedFigureDTO(
    val id: String,
    val caption: String?
)

// ── Asset (Phase 4) ──
data class AssetDTO(
    val model_id: String?,
    val model_name: String?,
    val description: String?,
    val model_url: String?,
    val thumbnail_url: String?,
    val rarity: String?,
    val source: String?
)

// ── Validation (Phase 2) ──
data class ValidationDTO(
    val is_valid: Boolean,
    val detected_domain: String?,
    val confidence: Double?,
    val reason: String?
)

// ── Job Status Polling (Phase 5) ──
data class JobStatusDTO(
    val job_id: String,
    val status: String,
    val quiz: List<QuizQuestionDTO>?,
    val quiz_status: String?,
    val tutor_script: String?,
    val tutor_status: String?,
    val summary: String?,
    val summary_status: String?,
    val asset_generation: List<AssetGenerationDTO>?,
    val assets_status: String?,
    val error: String?
)

data class QuizQuestionDTO(
    val question: String,
    val options: List<String>,
    val correct_answer: String
)

data class AssetGenerationDTO(
    val concept: String?,
    val generation_job_id: String?
)
