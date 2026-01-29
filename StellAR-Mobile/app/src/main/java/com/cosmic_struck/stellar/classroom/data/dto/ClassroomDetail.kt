package com.cosmic_struck.stellar.classroom.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassroomDetail(
    val name: String,
    val creator_name: String,
    val member_count: Long,
    val creator_id : String,
    val classroom_code: String
)
