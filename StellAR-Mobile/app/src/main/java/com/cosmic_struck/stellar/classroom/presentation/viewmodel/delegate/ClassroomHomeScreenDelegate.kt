package com.cosmic_struck.stellar.classroom.presentation.viewmodel.delegate

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.cosmic_struck.stellar.classroom.domain.usecase.GetClassroomDetailsUseCase
import com.cosmic_struck.stellar.classroom.domain.usecase.GetClassroomMembersUseCase
import com.cosmic_struck.stellar.classroom.domain.usecase.GetClassroomModelsUseCase
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ClassroomHomeScreenState
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.Options
import com.cosmic_struck.stellar.common.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class ClassroomHomeScreenDelegate @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val getClassroomDetailsUseCase: GetClassroomDetailsUseCase,
    private val getClassroomMembersUseCase: GetClassroomMembersUseCase,
    private val getClassroomModelsUseCase: GetClassroomModelsUseCase,
    private val savedStateHandle: SavedStateHandle
) {
    private val _state = MutableStateFlow(ClassroomHomeScreenState())
    val state: StateFlow<ClassroomHomeScreenState> = _state.asStateFlow()

    init {
        val classroomId = savedStateHandle.get<String>("classroom_id")
        Log.d("Classroom Id Checking",classroomId.toString())
        if (classroomId != null) {
            _state.update { it.copy(classroom_id = classroomId) }
        }
    }

    fun onToggle(ind: Int) {
        _state.update {
            it.copy(selected = if (ind == 0) Options.MEMBERS else Options.MODELS)
        }
    }

    // This function aggregates all data fetching
    suspend fun fetchAllClassroomData() {
        val classroomId = _state.value.classroom_id
        if (classroomId.isEmpty()) return

        // We run these concurrently so the UI doesn't wait for one to finish before starting the next
        coroutineScope {
            launch { fetchDetails(classroomId) }
            launch { fetchMembers(classroomId) }
            launch { fetchModels(classroomId) }
        }
    }


    private suspend fun fetchDetails(id: String) {
        val userId = supabaseClient.auth.retrieveUserForCurrentSession().id
        getClassroomDetailsUseCase(id).collect { resource ->
            _state.update {
                when (resource) {
                    is Resource.Loading -> it.copy(isLoading = true)
                    is Resource.Error -> it.copy(isLoading = false, error = resource.message)
                    is Resource.Success -> it.copy(
                        classroomName = resource.data?.name ?: "",
                        classroomAuthor = resource.data?.creator_name ?: "",
                        isLoading = false,
                        isCreator = resource.data?.creator_id == userId,
                        classroomCode = resource.data?.classroom_code.toString()
                    )
                }
            }
        }
    }

    private suspend fun fetchMembers(id: String) {
        getClassroomMembersUseCase(id).collect { resource ->
            _state.update {
                when (resource) {
                    is Resource.Loading -> it.copy(isLoading = true)
                    is Resource.Error -> it.copy(isLoading = false, error = resource.message)
                    is Resource.Success -> it.copy(
                        classroomMembersList = resource.data ?: emptyList(),
                        classroomMembers = resource.data?.size.toString(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun fetchModels(id: String) {
        getClassroomModelsUseCase(id).collect { resource ->
            _state.update {
                when (resource) {
                    is Resource.Loading -> it.copy(isLoading = true)
                    is Resource.Error -> it.copy(isLoading = false, error = resource.message)
                    is Resource.Success -> it.copy(
                        classroomModelsList = resource.data ?: emptyList(),
                        isLoading = false
                    )
                }
            }
        }
    }
}