package com.cosmic_struck.stellar.stellar.arlab.presentation.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.stellar.arlab.domain.model.PlanetComparatorModel
import com.cosmic_struck.stellar.stellar.arlab.presentation.component.ARPlanetComparatorScene
import com.cosmic_struck.stellar.stellar.arlab.presentation.states.Feedback
import com.cosmic_struck.stellar.stellar.arlab.presentation.viewmodel.PlanetComparatorViewModel

@Composable
fun PlanetsComparatorScreen(
    viewModel: PlanetComparatorViewModel = hiltViewModel<PlanetComparatorViewModel>(),
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier) {

    val state = viewModel.state.value
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            SimpleTopAppBar(
                title = "Planets Comparator",
                popNavigation = {
                    navigateBack()
                }
            )
        }
    ) {it->
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ){
            ARPlanetComparatorScene(
                leftPlanet = state.leftPlanet,
                rightPlanet = state.rightPlanet,
                onPlanetTapped = {
                    viewModel.onPlanetSelected(it)
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Which planet is larger?",
                    color = Color.Gray
                )

                Text(
                    text = "Score: ${state.score}",
                    color = Color.Gray
                )

            }
            state.feedback?.let { feedback ->
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    text = if (feedback == Feedback.CORRECT) "Correct!" else "Try again",
                    color = if (feedback == Feedback.CORRECT) Color.Green else Color.Red
                )
            }
        }
    }
}