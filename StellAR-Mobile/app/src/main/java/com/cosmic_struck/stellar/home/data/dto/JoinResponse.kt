package com.cosmic_struck.stellar.home.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class JoinResponse(
    val status: String,
    val message: String,
    val classroom_id: String? = null
)