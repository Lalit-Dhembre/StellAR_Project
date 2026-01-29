package com.cosmic_struck.stellar.classroom.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassroomMember(
    val user_id: String,
    val user_name: String,
    val level: Long,
    val total_xp: Double,
    val joined_at: String
)
