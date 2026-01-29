package com.cosmic_struck.stellar.auth.presentation

import android.net.Uri

data class AuthScreenState(
    val isLoading : Boolean = false,
    val email : String = "",
    val password: String = "",
    val username: String = "",
    val error : String = "",
    val profileImage: Uri? = null,
    val success: Boolean = false
)
