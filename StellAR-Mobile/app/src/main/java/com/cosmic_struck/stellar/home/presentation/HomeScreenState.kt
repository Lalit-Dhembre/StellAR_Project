package com.cosmic_struck.stellar.home.presentation

import com.cosmic_struck.stellar.home.data.dto.JoinedClassroom

data class HomeScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val options: List<Options> = listOf(Options.MODULES,Options.CLASSROOM),
    val selected : Options = Options.MODULES,
    val joinedClassrooms: List<JoinedClassroom> = emptyList(),
    val codeText : String = "",
    val modalSheetState : Boolean = false,
    val userName : String = "",
    val userLevel: String = "",
    val profile: String = "",
    val classroomJoinStatus: ClassroomJoinStatus = ClassroomJoinStatus.NOT_JOINED,
    val userCreatedClassrooms: List<JoinedClassroom> = emptyList(),
    // Create Classroom State
    val createClassroomModalState: Boolean = false,
    val classroomNameText: String = "",
    val classroomCreateStatus: ClassroomCreateStatus = ClassroomCreateStatus.NOT_CREATED,
)

enum class ClassroomJoinStatus(){
    JOINED,
    NOT_JOINED,
    ERROR
}

enum class ClassroomCreateStatus(){
    NOT_CREATED,
    CREATING,
    CREATED,
    ERROR
}

enum class Options(){
    MODULES,
    CLASSROOM
}

