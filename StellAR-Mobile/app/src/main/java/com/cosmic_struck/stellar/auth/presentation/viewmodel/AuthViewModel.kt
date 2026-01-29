package com.cosmic_struck.stellar.auth.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.auth.domain.LoginUseCase
import com.cosmic_struck.stellar.auth.domain.SignUpUseCase
import com.cosmic_struck.stellar.auth.presentation.AuthScreenState
import com.cosmic_struck.stellar.common.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(AuthScreenState())
    val state: StateFlow<AuthScreenState> = _state

    fun signUpWithEmail(){
        viewModelScope.launch {
            signUpUseCase(
                state.value.username,
                state.value.email,
                state.value.password,
                state.value.profileImage,
                context = application
            ).collect {
                when(it){
                    is Resource.Loading<*> -> {_state.value = _state.value.copy(isLoading = true) }
                    is Resource.Error<*> -> {_state.value = _state.value.copy(isLoading = false, error = it.message.toString())}
                    is Resource.Success<*> -> {_state.value = _state.value.copy(isLoading = false, success = true)}
                }
            }
        }
    }
    fun setProfileImage(uri: Uri?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                profileImage = uri
            )
        }
    }

    fun signInWithEmail(){
        viewModelScope.launch {
            loginUseCase(state.value.email,state.value.password).collect{
                when(it){
                    is Resource.Success<*> -> _state.value = _state.value.copy(isLoading = false, success = true)
                    is Resource.Error<*> -> _state.value = _state.value.copy(isLoading = false, error = it.message.toString())
                    is Resource.Loading<*> -> _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun setEmailAddress(email: String){
        viewModelScope.launch {
            _state.value = _state.value.copy(
                email = email
            )
        }
    }

    fun setPassword(password: String){
        viewModelScope.launch {
            _state.value = _state.value.copy(
                password = password
            )
        }
    }

    fun setUsername(username: String){
        viewModelScope.launch {
            _state.value = _state.value.copy(
                username = username
            )
        }
    }

}