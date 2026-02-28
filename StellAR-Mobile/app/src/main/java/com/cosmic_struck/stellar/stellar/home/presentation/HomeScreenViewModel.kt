package com.cosmic_struck.stellar.stellar.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.appwrite.services.Account
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StellarHomeScreenViewModel @Inject constructor(
    private val account: Account
) : ViewModel(){

    init {
        viewModelScope.launch {
            try {
                val user = account.get()
                Log.d("HomeScreenViewModel", "User: $user")
            } catch (e: Exception) {
                Log.d("HomeScreenViewModel", "No session: ${e.message}")
            }
        }
    }
}