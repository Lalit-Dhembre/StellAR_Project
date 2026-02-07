package com.cosmic_struck.stellar.home.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateClassroomResponse(
    val status: String,
    val message: String,
    val classroom_id: String? = null,
    val join_code: String? = null
)
