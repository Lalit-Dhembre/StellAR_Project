package com.cosmic_struck.stellar.home.presentation.viewmodel

import androidx.annotation.OptIn
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.home.domain.usecases.GetUserCreatedClassroom
import com.cosmic_struck.stellar.home.domain.usecases.GetUserJoinedClassroomsUseCase
import com.cosmic_struck.stellar.home.domain.usecases.GetUserProfileUseCase
import com.cosmic_struck.stellar.home.domain.usecases.JoinClassroomUseCase
import com.cosmic_struck.stellar.home.presentation.ClassroomJoinStatus
import com.cosmic_struck.stellar.home.presentation.HomeScreenState
import com.cosmic_struck.stellar.home.presentation.Options
import com.cosmic_struck.stellar.home.domain.usecases.UpdateUserProfilePictureUseCase
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val getUserJoinedClassroomsUseCase: GetUserJoinedClassroomsUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val joinClassroomUseCase: JoinClassroomUseCase,
    private val supabaseClient: SupabaseClient,
    private val savedStateHandle: SavedStateHandle,
    private val getUserCreatedClassroom: GetUserCreatedClassroom,
    private val updateUserProfilePictureUseCase: UpdateUserProfilePictureUseCase
): ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    init {
        getUser()
        getJoinedClassrooms()
//        getCreatedClassrooms()
    }


    fun toggleJoinClassroomStatus(){
        viewModelScope.launch {
            _state.value = _state.value.copy(
                classroomJoinStatus = ClassroomJoinStatus.NOT_JOINED
            )
        }
    }
    fun refresh(){
        viewModelScope.launch {
            getUser()
            getJoinedClassrooms()
        }
    }
    fun setCode(code: String){
        _state.value = _state.value.copy(codeText = code)
    }

    fun changeModalSheetState(){
        _state.value = _state.value.copy(modalSheetState = !_state.value.modalSheetState)
    }
    fun joinClassroom(){
        viewModelScope.launch {
            val userId = supabaseClient.auth.retrieveUserForCurrentSession().id
            joinClassroomUseCase(userId,state.value.codeText ).collect {
                when(it){
                    is Resource.Loading<*> -> _state.value = _state.value.copy(isLoading = true)

                    is Resource.Error<*> -> {_state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message,
                        classroomJoinStatus = ClassroomJoinStatus.ERROR
                    )
                    Log.d("HOME SCREEN VIEWMODEL",it.message.toString())
                    }

                    is Resource.Success<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            classroomJoinStatus = if(it.data != null) ClassroomJoinStatus.JOINED else ClassroomJoinStatus.ERROR
                        )
                        getJoinedClassrooms()
                    }
                }
            }
        }
    }




    fun getUser(){
        viewModelScope.launch {
            val userId = supabaseClient.auth.retrieveUserForCurrentSession().id
            getUserProfileUseCase(userId).collect{it->
                when(it){
                    is Resource.Loading<*> -> _state.value = _state.value.copy(isLoading = true)

                    is Resource.Error<*> -> _state.value = _state.value.copy(error = it.message, isLoading = false)

                    is Resource.Success<*> -> _state.value = _state.value.copy(
                          userName = it.data?.user_name.toString(),
                          userLevel = it.data?.level.toString(),
                        profile = it.data?.user_pp.toString(),
                         isLoading = false)
                }
            }
        }
    }
    fun getCreatedClassrooms(){
        viewModelScope.launch {
            val userId = supabaseClient.auth.retrieveUserForCurrentSession().id
            getUserCreatedClassroom(userId).collect {it->
                when(it){
                    is Resource.Error<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error =  it.message
                        )
                    }
                    is Resource.Loading<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = true
                        )
                    }
                    is Resource.Success<*> -> _state.value.copy(
                        isLoading = false,
                        userCreatedClassrooms = it.data ?: emptyList()
                    )
                }
            }
        }
    }
    @OptIn(UnstableApi::class)
    fun getJoinedClassrooms(){
        viewModelScope.launch {
            val userId = supabaseClient.auth.retrieveUserForCurrentSession().id
            Log.d("UserId",userId.toString())
            getUserJoinedClassroomsUseCase(userId).collect { it->
                when (it){
                    is Resource.Loading<*> -> _state.value = _state.value.copy(isLoading = true)

                    is Resource.Error<*> -> _state.value = _state.value.copy(error = it.message, isLoading = false)

                    is Resource.Success<*> -> _state.value = _state.value.copy(joinedClassrooms = it.data ?: emptyList(), isLoading = false)

                }
            }
        }
    }
    fun onToggle(ind: Int){
        if(ind == 0){
            _state.value = _state.value.copy(selected = Options.MODULES)
        }
        else{
            _state.value = _state.value.copy(selected = Options.CLASSROOM)
        }
    }

    fun saveClassroomId(classroomId: String){
        viewModelScope.launch {
            savedStateHandle["classroom_id"] = classroomId
        }
    }

    fun updateProfilePicture(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val userId = supabaseClient.auth.retrieveUserForCurrentSession().id
                updateUserProfilePictureUseCase(userId, uri, context).collect { result ->
                    when (result) {
                        is Resource.Loading -> _state.value = _state.value.copy(isLoading = true)
                        is Resource.Success -> {
                            // Append timestamp to force refresh the image in UI
                            val freshUrl = (result.data ?: "") + "?t=${System.currentTimeMillis()}"
                            _state.value = _state.value.copy(
                                isLoading = false,
                                profile = freshUrl
                            )
                        }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}