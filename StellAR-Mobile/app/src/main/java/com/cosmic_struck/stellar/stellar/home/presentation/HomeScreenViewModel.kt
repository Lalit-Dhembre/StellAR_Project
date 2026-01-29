package com.cosmic_struck.stellar.stellar.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StellarHomeScreenViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel(){

    init {
        val auth = supabaseClient.auth
        viewModelScope.launch {
            val user = auth.retrieveUserForCurrentSession(updateSession = true)
            Log.d("HomeScreenViewModel", "User: $user")
        }
    }
}