package com.cosmic_struck.stellar.stellar.scantext.data.dto

data class ScanDTO(
    val success: Boolean,
    val count: Int,
    val documents: List<LangchainDocument>? = null,
    val error: String? = null,
    val domain_match: Boolean? = null,
    val detected_domain: String? = null,
    val message: String? = null,
    val reason: String? = null
)

data class LangchainDocument(
    val page_content: String,
    val metadata: Map<String, String>? = null
)
