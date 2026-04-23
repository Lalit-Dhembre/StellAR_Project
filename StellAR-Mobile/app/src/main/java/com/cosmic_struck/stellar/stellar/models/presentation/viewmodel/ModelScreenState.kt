package com.cosmic_struck.stellar.stellar.models.presentation.viewmodel

import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import io.github.sceneview.node.ModelNode

data class ModelHomeScreenState(
    val currentIndex: Int = 0,
    val currentList: ListType = ListType.MY_COLLECTION,
    val subjectModelList: List<ClassroomModel> = emptyList(),
    val collectedList: ArrayList<ClassroomModel> = ArrayList(),
    val discoverList: ArrayList<ClassroomModel> = ArrayList(),
    val isLoadingHome: Boolean = false,
    val homeError: String = "",
    val minLevel: Int = 10,
)

data class ModelViewScreenState(
    val rotationSpeed: Float = 0.5f,
    val cameraDistance: Float = 3.5f,
    val rotationAngle: Float = 0f,
    val modelNode: ModelNode? = null,
    val modelTitle: String = "",
    val isLoadingModel: Boolean = false,
    val modelError: String = "",
    val modelURL: String = "",
    val modelPath: String = "",
    val scene: SceneType = SceneType.SceneView,
    // Narration state
    val narrationScript: String = "",
    val isNarrating: Boolean = false,
    val isPaused: Boolean = false,
    val narrationProgress: Float = 0f  // 0.0 to 1.0
)

enum class ListType {
    MY_COLLECTION,
    DISCOVER
}

enum class SceneType {
    ARSceneView,
    SceneView
}


