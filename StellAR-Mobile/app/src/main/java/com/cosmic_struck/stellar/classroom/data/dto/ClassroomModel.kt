package com.cosmic_struck.stellar.classroom.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassroomModel(
    val model_id: String,
    val model_name: String,
    val description: String?,
    val model_url: String,
    val rarity: String,
    val xp_reward: Int?,
    val model_subject: String,
    val model_thumbnail: String?,
    val min_level: Int,
    val created_at: String
)
