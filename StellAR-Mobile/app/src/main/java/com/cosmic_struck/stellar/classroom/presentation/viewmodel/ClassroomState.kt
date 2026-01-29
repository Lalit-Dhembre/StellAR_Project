package com.cosmic_struck.stellar.classroom.presentation.viewmodel

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomMember
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModule


data class ClassroomHomeScreenState(
    val classroom_id : String = "",
    val classroomName: String = "",
    val classroomAuthor : String = "",
    val classroomMembers: String = "",
    val classroomCode: String = "",
    val classroomMembersList: List<ClassroomMember> = emptyList(),
    val classroomModelsList: List<ClassroomModule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val options: List<Options> = listOf(Options.MEMBERS,Options.MODELS),
    val selected : Options = Options.MEMBERS,
    val isCreator: Boolean = false
)

enum class Options{
    MEMBERS,
    MODELS
}