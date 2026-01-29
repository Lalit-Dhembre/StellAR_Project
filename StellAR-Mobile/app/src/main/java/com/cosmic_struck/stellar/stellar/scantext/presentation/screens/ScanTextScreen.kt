package com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.CameraScanCaption
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.CameraScanText
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.RectangleFrame
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.ScanButtonScanText
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.TopBarScanTextBook
import com.cosmic_struck.stellar.ui.theme.Blue4
import com.cosmic_struck.stellar.ui.theme.Blue5

@Composable
fun ScanTextScreen(

    viewModel: ScanTextViewModel = hiltViewModel<ScanTextViewModel>(),
    navigateBack: () -> Unit,
    navigateToResults: () -> Unit,
    modifier: Modifier = Modifier) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Blue4,
                        Blue5
                    )
                )
            ),
        containerColor = Color.Transparent,
        topBar = {
            TopBarScanTextBook(title = "Scan Screen",navigateBack)
        }
    ) {it->

        LaunchedEffect(state.scanResults, state.switchToResults) {
            Log.d("NAVIGATION_DEBUG", "LaunchedEffect triggered")
            Log.d("NAVIGATION_DEBUG", "scanResults: ${state.scanResults?.count}")
            Log.d("NAVIGATION_DEBUG", "switchToResults: ${state.switchToResults}")

            if (state.scanResults != null && state.switchToResults) {
                Log.d("NAVIGATION_DEBUG", "Both conditions met, navigating...")
                navigateToResults()
                Log.d("NAVIGATION_DEBUG", "Navigate called")
            } else {
                Log.d("NAVIGATION_DEBUG", "Conditions not met yet")
            }
        }

        if(state.isLoading){
            Box(
                modifier = Modifier
                    .fillMaxSize()){
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }
        else{
            Box(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ){
                CameraScanText(
                    imageCapture = state.imageCapture,
                    modifier = Modifier
                        .fillMaxSize()
                )
                CameraScanCaption(
                    modifier = Modifier
                        .align(
                            Alignment.Center
                        )
                )
                RectangleFrame(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
                ScanButtonScanText(
                    { viewModel.captureImage(
                        context = context,
                        imageCapture = state.imageCapture,
                        onImageCaptured = { file ->
                            viewModel.uploadImage(file)
                        }
                    ) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                )
            }
        }


    }
}