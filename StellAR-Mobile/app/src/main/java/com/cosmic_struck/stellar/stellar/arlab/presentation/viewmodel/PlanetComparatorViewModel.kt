package com.cosmic_struck.stellar.stellar.arlab.presentation.viewmodel


import androidx.lifecycle.ViewModel
import com.cosmic_struck.stellar.stellar.arlab.domain.model.PlanetComparatorModel
import com.cosmic_struck.stellar.stellar.arlab.presentation.states.Feedback
import com.cosmic_struck.stellar.stellar.arlab.presentation.states.PlanetComparatorState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlanetComparatorViewModel @Inject constructor(
    ) : ViewModel(){
        private val _state = MutableStateFlow(PlanetComparatorState())
        val state: StateFlow<PlanetComparatorState> = _state
        private val planets = listOf(
            PlanetComparatorModel("earth", "Earth", 6371f, "models/earth.glb"),
            PlanetComparatorModel("mars", "Mars", 3389f, "models/mars.glb"),
            PlanetComparatorModel("jupiter", "Jupiter", 69911f, "models/jupiter.glb"),
            PlanetComparatorModel("saturn", "Saturn", 58232f, "models/saturn.glb")
    )
    init {
        startNewRound()
    }

    fun startNewRound() {
        val (left, right) = planets.shuffled().take(2)

        val correctId =
            if (left.radiusKm > right.radiusKm) left.id else right.id

        _state.value = _state.value.copy(
            leftPlanet = left,
            rightPlanet = right,
            correctPlanetId = correctId,
            feedback = null
        )
    }

    fun onPlanetSelected(planetId: String) {
        val isCorrect = planetId == _state.value.correctPlanetId

        _state.value = _state.value.copy(
            score = if (isCorrect) _state.value.score + 1 else _state.value.score,
            feedback = if (isCorrect) Feedback.CORRECT else Feedback.WRONG
        )
    }

    fun nextRound() {
        _state.value = _state.value.copy(
            round = _state.value.round + 1
        )
        startNewRound()
    }
}


//2026-01-15 22:33:26.561 12263-12263 Filament                com.cosmic_struck.stellar            E  Unable to parse glTF file.
//2026-01-15 22:33:26.562 12263-12263 AndroidRuntime          com.cosmic_struck.stellar            D  Shutting down VM
//2026-01-15 22:33:26.563 12263-12263 AndroidRuntime          com.cosmic_struck.stellar            E  FATAL EXCEPTION: main
//                                                                                                    Process: com.cosmic_struck.stellar, PID: 12263
//                                                                                                    java.lang.NullPointerException
//                                                                                                    	at io.github.sceneview.loaders.ModelLoader.createModel(ModelLoader.kt:63)
//                                                                                                    	at io.github.sceneview.loaders.ModelLoader.createModel(ModelLoader.kt:78)
//                                                                                                    	at io.github.sceneview.loaders.ModelLoader.createModelInstance(ModelLoader.kt:169)
//                                                                                                    	at io.github.sceneview.loaders.ModelLoader.createModelInstance$default(ModelLoader.kt:164)
//                                                                                                    	at com.cosmic_struck.stellar.stellar.arlab.presentation.component.PlanetComparatorSceneKt.ARPlanetComparatorScene$lambda$7$0$createPlanetNode(PlanetComparatorScene.kt:82)
//                                                                                                    	at com.cosmic_struck.stellar.stellar.arlab.presentation.component.PlanetComparatorSceneKt.ARPlanetComparatorScene$lambda$7$0(PlanetComparatorScene.kt:108)
//                                                                                                    	at com.cosmic_struck.stellar.stellar.arlab.presentation.component.PlanetComparatorSceneKt.$r8$lambda$VuYQohTtg3pBvELasnL7Pgn1qeI(Unknown Source:0)
//                                                                                                    	at com.cosmic_struck.stellar.stellar.arlab.presentation.component.PlanetComparatorSceneKt$$ExternalSyntheticLambda1.invoke(D8$$SyntheticClass:0)
//                                                                                                    	at io.github.sceneview.SceneKt$rememberOnGestureListener$20$1$1.onSingleTapConfirmed(Scene.kt:533)
//                                                                                                    	at io.github.sceneview.gesture.GestureDetector$gestureDetector$1.onSingleTapConfirmed(GestureDetector.kt:111)
//                                                                                                    	at android.view.GestureDetector$GestureHandler.handleMessage(GestureDetector.java:342)
//                                                                                                    	at android.os.Handler.dispatchMessage(Handler.java:112)
//                                                                                                    	at android.os.Looper.loopOnce(Looper.java:268)
//                                                                                                    	at android.os.Looper.loop(Looper.java:384)
//                                                                                                    	at android.app.ActivityThread.main(ActivityThread.java:8921)
//                                                                                                    	at java.lang.reflect.Method.invoke(Native Method)
//                                                                                                    	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:580)
//                                                                                                    	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:907)
//2026-01-15 22:33:26.582 12263-12263 Process                 com.cosmic_struck.stellar            I  Sending signal. PID: 12263 SIG: 9
//---------------------------- PROCESS ENDED (12263) for package com.cosmic_struck.stellar ----------------------------
//2026-01-15 22:33:26.704 22043-24956 AppOps                  system_server                        E  Operation not started: uid=10414 pkg=com.cosmic_struck.stellar(null) op=CAMERA