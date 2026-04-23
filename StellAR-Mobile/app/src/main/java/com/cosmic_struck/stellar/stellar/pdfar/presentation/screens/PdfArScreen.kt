package com.cosmic_struck.stellar.stellar.pdfar.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.stellar.pdfar.presentation.PdfArUiState
import com.cosmic_struck.stellar.stellar.pdfar.presentation.PdfArViewModel
import java.io.File

private const val TAG = "PdfArScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfArScreen(
    domain: String,
    onNavigateBack: () -> Unit,
    onModelReady: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfArViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Track what we've already handled to prevent re-triggering
    var lastHandledError by remember { mutableStateOf<String?>(null) }
    var lastHandledModelUrl by remember { mutableStateOf<String?>(null) }

    // Handle side-effects (errors + model-ready navigation)
    LaunchedEffect(uiState) {
        val state = uiState
        Log.d(TAG, "LaunchedEffect triggered: state=${state::class.simpleName}")

        when (state) {
            is PdfArUiState.Error -> {
                // Only show the snackbar once per unique error message
                if (state.message != lastHandledError) {
                    Log.d(TAG, "Showing error snackbar: ${state.message}")
                    lastHandledError = state.message
                    snackbarHostState.showSnackbar(
                        message = state.message,
                        duration = SnackbarDuration.Long
                    )
                    // Return to concept list (not Idle)
                    viewModel.clearError()
                }
            }
            is PdfArUiState.ModelReady -> {
                // Only navigate once per unique model URL
                if (state.modelUrl != lastHandledModelUrl) {
                    Log.d(TAG, "Model ready — navigating to AR: url=${state.modelUrl}, name=${state.entityName}")
                    lastHandledModelUrl = state.modelUrl
                    onModelReady(state.modelUrl, state.entityName, state.script)
                    // Reset back to content list (not Idle)
                    viewModel.resetState()
                }
            }
            else -> {
                // Reset tracking when we're in a non-terminal state
                if (state !is PdfArUiState.GeneratingModel) {
                    lastHandledError = null
                    lastHandledModelUrl = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR Generator") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Back button pressed. Current state: ${uiState::class.simpleName}")
                        onNavigateBack()
                    }) {
                        Text("←") 
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PdfArUiState.Idle -> {
                    Log.d(TAG, "Rendering: Idle (upload screen)")
                    PdfUploadScreen(
                        onPdfSelected = { file ->
                            viewModel.processPdf(file, domain)
                        }
                    )
                }
                is PdfArUiState.Uploading -> {
                    Log.d(TAG, "Rendering: Uploading")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PdfArUiState.ContentLoaded -> {
                    Log.d(TAG, "Rendering: ContentLoaded (${state.concepts.size} concepts, ${state.nativeImages.size} images)")
                    SectionListScreen(
                        concepts = state.concepts,
                        nativeImages = state.nativeImages,
                        onConceptClick = { conceptId, entityName ->
                            Log.d(TAG, "Concept clicked: id=$conceptId, name=$entityName")
                            viewModel.fetchConceptDetails(conceptId, entityName)
                        },
                        onNativeImageClick = { imageUrl, entityName ->
                            Log.d(TAG, "Native image clicked: name=$entityName")
                            viewModel.handleNativeImageSelection(imageUrl, entityName)
                        }
                    )
                }
                is PdfArUiState.GeneratingModel -> {
                    Log.d(TAG, "Rendering: GeneratingModel (loading screen)")
                    ModelLoadingScreen()
                }
                is PdfArUiState.ModelReady -> {
                    Log.d(TAG, "Rendering: ModelReady (waiting for LaunchedEffect)")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Model is ready! Opening AR...")
                    }
                }
                is PdfArUiState.Error -> {
                    // Show the content list if we have one, otherwise show upload screen
                    // This prevents the "navigate back" feeling on errors
                    Log.d(TAG, "Rendering: Error state — showing snackbar, keeping current view")
                    PdfUploadScreen(
                        onPdfSelected = { file ->
                            viewModel.processPdf(file, domain)
                        }
                    )
                }
            }
        }
    }
}
