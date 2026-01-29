package com.cosmic_struck.stellar.home.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val created_at: String,
    val user_name: String = "User",
    val level: Long = 1,
    val total_xp: Double = 0.0,
    val user_pp: String = ""
)
