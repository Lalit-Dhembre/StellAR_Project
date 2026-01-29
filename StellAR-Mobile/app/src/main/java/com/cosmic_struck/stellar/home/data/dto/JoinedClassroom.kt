package com.cosmic_struck.stellar.home.data.dto

import kotlinx.serialization.Serializable


@Serializable
data class JoinedClassroom(
    val classroom_id: String,
    val classroom_name: String,
    val join_code: String,
    val created_at: String,
    val creator_name: String,
    val member_count: Long,
    val is_creator: Boolean
)
