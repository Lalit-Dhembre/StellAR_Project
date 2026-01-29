package com.cosmic_struck.stellar.stellar.models.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.stellar.models.domain.usecase.GetModelsBySubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getModelsBySubjectUseCase: GetModelsBySubjectUseCase
): ViewModel() {
    private var _state = mutableStateOf(ModelHomeScreenState())
    val state : State<ModelHomeScreenState> = _state

    init {
        getModelsBySubject(subject = "Planet")
    }


    fun listSegregator(models : List<ClassroomModel>){
        models.map {
            if (it.min_level <= state.value.minLevel) {
                _state.value.collectedList.add(it)
            } else {
                _state.value.discoverList.add(it)
            }
        }
        Log.d("LIST CHECKING SE",_state.value.collectedList.toString() + " " + _state.value.discoverList.toString())
    }

    fun getModelsBySubject(subject: String) {
        viewModelScope.launch {
            getModelsBySubjectUseCase(subject).collect { resource ->
                when(resource){
                    is Resource.Loading->{
                        _state.value = _state.value.copy(
                            isLoadingHome = true
                        )
                    }
                    is Resource.Success ->{
                        _state.value = _state.value.copy(
                            subjectModelList = resource.data ?: emptyList(),
                            isLoadingHome = false
                        )
                        listSegregator(state.value.subjectModelList)
                    }
                    is Resource.Error->{
                        _state.value = _state.value.copy(
                            isLoadingHome = false,
                            homeError = resource.message.toString()
                        )
                    }
                }
//                _state.update {
//                    when (resource) {
//                        is Resource.Loading -> it.copy(isLoadingHome = true)
//                        is Resource.Error -> it.copy(isLoadingHome = false, homeError = resource.message.toString())
//                        is Resource.Success -> {
//                            it.copy(
//                                subjectModelList = resource.data ?: emptyList(),
//                                isLoadingHome = false
//                            )
//
//                        }
//                    }
//                }
                Log.d("LIST CHECKING",_state.value.subjectModelList.toString())
                listSegregator(state.value.subjectModelList)
            }
        }
    }

    fun onToggleList(){
        viewModelScope.launch {
            _state.value = _state.value.copy(
                currentIndex = if(_state.value.currentIndex == 0) 1 else 0,
                currentList = if(_state.value.currentList == ListType.MY_COLLECTION) ListType.DISCOVER else ListType.MY_COLLECTION
            )
        }
    }

}